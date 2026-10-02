package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.documentfile.provider.DocumentFile
import com.example.data.api.GitHubApiService
import com.example.data.db.AppDatabase
import com.example.data.db.PushHistoryEntity
import com.example.data.db.UserAuthEntity
import com.example.data.model.CreateRefRequest
import com.example.data.model.CreateRepoRequest
import com.example.data.model.GitBlobRequest
import com.example.data.model.GitCommitRequest
import com.example.data.model.GitTreeEntry
import com.example.data.model.GitTreeRequest
import com.example.data.model.GitHubRepo
import com.example.data.model.GitHubUser
import com.example.data.model.LocalScannedFile
import com.example.data.model.UpdateRefRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

sealed class PushProgress {
    data class Scanning(val message: String) : PushProgress()
    data class UploadingBlob(val index: Int, val total: Int, val currentFile: String, val percent: Float) : PushProgress()
    data class CreatingTree(val message: String) : PushProgress()
    data class CreatingCommit(val message: String) : PushProgress()
    data class UpdatingBranch(val message: String) : PushProgress()
    data class Completed(val commitSha: String, val commitUrl: String?, val fileCount: Int) : PushProgress()
    data class Failed(val error: String) : PushProgress()
}

class GitHubRepository(
    private val context: Context,
    private val api: GitHubApiService,
    private val database: AppDatabase
) {
    val savedAuth: Flow<UserAuthEntity?> = database.userAuthDao().getSavedAuth()
    val pushHistory: Flow<List<PushHistoryEntity>> = database.pushHistoryDao().getAllHistory()

    private fun formatAuthHeader(token: String): String {
        val trimmed = token.trim()
        return if (trimmed.startsWith("Bearer ") || trimmed.startsWith("token ")) {
            trimmed
        } else {
            "token $trimmed"
        }
    }

    suspend fun verifyAndSaveToken(token: String): Result<GitHubUser> = withContext(Dispatchers.IO) {
        try {
            val authHeader = formatAuthHeader(token)
            val user = api.getAuthenticatedUser(authHeader)
            database.userAuthDao().saveAuth(
                UserAuthEntity(
                    id = 1,
                    token = token.trim(),
                    username = user.login,
                    name = user.name,
                    avatarUrl = user.avatarUrl
                )
            )
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        database.userAuthDao().clearAuth()
    }

    suspend fun getUserRepositories(token: String): Result<List<GitHubRepo>> = withContext(Dispatchers.IO) {
        try {
            val authHeader = formatAuthHeader(token)
            val repos = api.getUserRepos(authHeader)
            Result.success(repos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRepository(
        token: String,
        name: String,
        description: String?,
        isPrivate: Boolean,
        autoInit: Boolean
    ): Result<GitHubRepo> = withContext(Dispatchers.IO) {
        try {
            val authHeader = formatAuthHeader(token)
            val repo = api.createRepo(
                authHeader,
                CreateRepoRequest(
                    name = name,
                    description = description,
                    private = isPrivate,
                    autoInit = autoInit
                )
            )
            Result.success(repo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun scanDirectory(
        treeUri: Uri,
        ignoreBuildDirs: Boolean = true
    ): Result<List<LocalScannedFile>> = withContext(Dispatchers.IO) {
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext Result.failure(Exception("Cannot access selected folder"))

            val ignoredNames = if (ignoreBuildDirs) {
                setOf(
                    ".git", ".gradle", ".idea", "build", "node_modules",
                    ".dart_tool", "bin", "obj", ".DS_Store", "Thumbs.db"
                )
            } else {
                setOf(".git") // Always ignore .git to avoid corrupting repo metadata
            }

            val filesList = mutableListOf<LocalScannedFile>()

            fun traverse(doc: DocumentFile, relativeParent: String) {
                val children = doc.listFiles()
                for (child in children) {
                    val name = child.name ?: continue
                    if (ignoredNames.contains(name)) {
                        continue
                    }
                    if (child.isDirectory) {
                        val nextParent = if (relativeParent.isEmpty()) name else "$relativeParent/$name"
                        traverse(child, nextParent)
                    } else if (child.isFile) {
                        val filePath = if (relativeParent.isEmpty()) name else "$relativeParent/$name"
                        filesList.add(
                            LocalScannedFile(
                                uriString = child.uri.toString(),
                                relativePath = filePath,
                                name = name,
                                sizeBytes = child.length(),
                                mimeType = child.type
                            )
                        )
                    }
                }
            }

            traverse(rootDoc, "")
            Result.success(filesList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pushFilesToRepo(
        token: String,
        owner: String,
        repo: String,
        branch: String,
        folderName: String,
        commitMessage: String,
        targetSubfolder: String,
        files: List<LocalScannedFile>,
        onProgress: (PushProgress) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        val authHeader = formatAuthHeader(token)
        var totalBytes = 0L
        for (f in files) {
            totalBytes += f.sizeBytes
        }

        try {
            if (files.isEmpty()) {
                throw Exception("No files to upload in selected folder")
            }

            // Step 1: Check branch ref and base commit
            onProgress(PushProgress.Scanning("Checking repository branch '$branch'..."))

            var baseCommitSha: String? = null
            var baseTreeSha: String? = null
            var branchExists = false

            try {
                val refResponse = api.getBranchRef(authHeader, owner, repo, branch)
                if (refResponse.isSuccessful && refResponse.body() != null) {
                    branchExists = true
                    baseCommitSha = refResponse.body()!!.obj.sha
                }
            } catch (e: Exception) {
                // Branch might not exist yet (e.g. empty repo)
                branchExists = false
            }

            // Step 2: Upload blobs
            val treeEntries = mutableListOf<GitTreeEntry>()
            val totalFiles = files.size

            for ((index, fileItem) in files.withIndex()) {
                val progressFloat = (index.toFloat() / totalFiles) * 0.7f // Blobs take 70% of progress
                onProgress(
                    PushProgress.UploadingBlob(
                        index = index + 1,
                        total = totalFiles,
                        currentFile = fileItem.relativePath,
                        percent = progressFloat
                    )
                )

                val fileUri = Uri.parse(fileItem.uriString)
                val fileBytes = context.contentResolver.openInputStream(fileUri)?.use { input ->
                    input.readBytes()
                } ?: throw Exception("Failed to read file: ${fileItem.name}")

                val base64Content = Base64.encodeToString(fileBytes, Base64.NO_WRAP)
                val blobResponse = api.createBlob(
                    authHeader,
                    owner,
                    repo,
                    GitBlobRequest(content = base64Content, encoding = "base64")
                )

                val repoFilePath = if (targetSubfolder.isBlank()) {
                    fileItem.relativePath
                } else {
                    val cleanSubfolder = targetSubfolder.trim().trim('/')
                    "$cleanSubfolder/${fileItem.relativePath}"
                }

                treeEntries.add(
                    GitTreeEntry(
                        path = repoFilePath,
                        mode = "100644",
                        type = "blob",
                        sha = blobResponse.sha
                    )
                )
            }

            // Step 3: Create Git Tree
            onProgress(PushProgress.CreatingTree("Creating Git tree hierarchy on GitHub..."))
            val treeRequest = GitTreeRequest(
                baseTree = if (branchExists) baseCommitSha else null,
                tree = treeEntries
            )
            val treeResponse = api.createTree(authHeader, owner, repo, treeRequest)

            // Step 4: Create Commit
            onProgress(PushProgress.CreatingCommit("Creating commit: '$commitMessage'..."))
            val commitRequest = GitCommitRequest(
                message = commitMessage,
                tree = treeResponse.sha,
                parents = if (baseCommitSha != null) listOf(baseCommitSha) else emptyList()
            )
            val commitResponse = api.createCommit(authHeader, owner, repo, commitRequest)

            // Step 5: Update or create branch ref
            onProgress(PushProgress.UpdatingBranch("Updating branch '$branch'..."))
            if (branchExists) {
                api.updateBranchRef(
                    authHeader,
                    owner,
                    repo,
                    branch,
                    UpdateRefRequest(sha = commitResponse.sha, force = true)
                )
            } else {
                api.createBranchRef(
                    authHeader,
                    owner,
                    repo,
                    CreateRefRequest(ref = "refs/heads/$branch", sha = commitResponse.sha)
                )
            }

            val commitUrl = "https://github.com/$owner/$repo/commit/${commitResponse.sha}"

            // Save history
            database.pushHistoryDao().insertHistory(
                PushHistoryEntity(
                    repoOwner = owner,
                    repoName = repo,
                    branch = branch,
                    folderName = folderName,
                    commitMessage = commitMessage,
                    commitSha = commitResponse.sha,
                    commitUrl = commitUrl,
                    fileCount = files.size,
                    totalBytes = totalBytes,
                    status = "SUCCESS",
                    timestamp = System.currentTimeMillis()
                )
            )

            onProgress(
                PushProgress.Completed(
                    commitSha = commitResponse.sha,
                    commitUrl = commitUrl,
                    fileCount = files.size
                )
            )

            Result.success(commitResponse.sha)
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Unknown push error"
            database.pushHistoryDao().insertHistory(
                PushHistoryEntity(
                    repoOwner = owner,
                    repoName = repo,
                    branch = branch,
                    folderName = folderName,
                    commitMessage = commitMessage,
                    commitSha = null,
                    commitUrl = null,
                    fileCount = files.size,
                    totalBytes = totalBytes,
                    status = "FAILED",
                    errorMessage = errorMsg,
                    timestamp = System.currentTimeMillis()
                )
            )
            onProgress(PushProgress.Failed(errorMsg))
            Result.failure(e)
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        database.pushHistoryDao().clearAll()
    }

    suspend fun deleteHistoryItem(id: Long) = withContext(Dispatchers.IO) {
        database.pushHistoryDao().deleteById(id)
    }
}
