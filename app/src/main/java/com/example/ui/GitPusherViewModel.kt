package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GitHubApiService
import com.example.data.db.AppDatabase
import com.example.data.db.PushHistoryEntity
import com.example.data.db.UserAuthEntity
import com.example.data.model.GitHubRepo
import com.example.data.model.GitHubUser
import com.example.data.model.LocalScannedFile
import com.example.data.repository.GitHubRepository
import com.example.data.repository.PushProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: GitHubUser, val token: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class PushUiState {
    object Idle : PushUiState()
    data class Pushing(
        val message: String,
        val currentIndex: Int = 0,
        val totalFiles: Int = 0,
        val progressPercent: Float = 0f,
        val currentFile: String = ""
    ) : PushUiState()
    data class Success(val commitSha: String, val commitUrl: String?, val fileCount: Int) : PushUiState()
    data class Error(val message: String) : PushUiState()
}

class GitPusherViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val api = GitHubApiService.create()
    val repository = GitHubRepository(application, api, database)

    // Auth State
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val savedAuth: StateFlow<UserAuthEntity?> = repository.savedAuth.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val pushHistory: StateFlow<List<PushHistoryEntity>> = repository.pushHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Repositories State
    private val _userRepos = MutableStateFlow<List<GitHubRepo>>(emptyList())
    val userRepos: StateFlow<List<GitHubRepo>> = _userRepos.asStateFlow()

    private val _isLoadingRepos = MutableStateFlow(false)
    val isLoadingRepos: StateFlow<Boolean> = _isLoadingRepos.asStateFlow()

    // Push Form State
    var tokenInput = MutableStateFlow("")
    var ownerInput = MutableStateFlow("")
    var repoNameInput = MutableStateFlow("")
    var branchInput = MutableStateFlow("main")
    var commitMessageInput = MutableStateFlow("Upload files via GitPusher")
    var targetSubfolderInput = MutableStateFlow("")
    var ignoreBuildDirs = MutableStateFlow(true)

    // Selected Folder State
    private val _selectedFolderUri = MutableStateFlow<Uri?>(null)
    val selectedFolderUri: StateFlow<Uri?> = _selectedFolderUri.asStateFlow()

    private val _selectedFolderName = MutableStateFlow<String?>(null)
    val selectedFolderName: StateFlow<String?> = _selectedFolderName.asStateFlow()

    private val _scannedFiles = MutableStateFlow<List<LocalScannedFile>>(emptyList())
    val scannedFiles: StateFlow<List<LocalScannedFile>> = _scannedFiles.asStateFlow()

    private val _isScanningFiles = MutableStateFlow(false)
    val isScanningFiles: StateFlow<Boolean> = _isScanningFiles.asStateFlow()

    // Push State
    private val _pushUiState = MutableStateFlow<PushUiState>(PushUiState.Idle)
    val pushUiState: StateFlow<PushUiState> = _pushUiState.asStateFlow()

    // Create Repo Dialog
    private val _isCreatingRepo = MutableStateFlow(false)
    val isCreatingRepo: StateFlow<Boolean> = _isCreatingRepo.asStateFlow()

    init {
        // Observe saved credentials
        viewModelScope.launch {
            savedAuth.collect { auth ->
                if (auth != null && _authState.value !is AuthState.Authenticated) {
                    tokenInput.value = auth.token
                    ownerInput.value = auth.username
                    verifyToken(auth.token)
                }
            }
        }
    }

    fun verifyToken(token: String) {
        if (token.isBlank()) {
            _authState.value = AuthState.Error("Please enter a valid GitHub Personal Access Token")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.verifyAndSaveToken(token)
            result.onSuccess { user ->
                _authState.value = AuthState.Authenticated(user, token)
                if (ownerInput.value.isBlank()) {
                    ownerInput.value = user.login
                }
                loadUserRepositories(token)
            }.onFailure { error ->
                _authState.value = AuthState.Error(
                    error.localizedMessage ?: "Failed to authenticate with GitHub. Check your token permissions."
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _authState.value = AuthState.Idle
            _userRepos.value = emptyList()
            tokenInput.value = ""
            ownerInput.value = ""
            repoNameInput.value = ""
        }
    }

    fun loadUserRepositories(tokenOverride: String? = null) {
        val currentToken = tokenOverride ?: (authState.value as? AuthState.Authenticated)?.token
        if (currentToken.isNullOrBlank()) return

        viewModelScope.launch {
            _isLoadingRepos.value = true
            val result = repository.getUserRepositories(currentToken)
            result.onSuccess { repos ->
                _userRepos.value = repos
            }.onFailure {
                // Keep existing repos or show error
            }
            _isLoadingRepos.value = false
        }
    }

    fun selectRepo(repo: GitHubRepo) {
        ownerInput.value = repo.fullName.substringBefore('/')
        repoNameInput.value = repo.name
        branchInput.value = repo.defaultBranch ?: "main"
    }

    fun onFolderSelected(uri: Uri, folderName: String) {
        _selectedFolderUri.value = uri
        _selectedFolderName.value = folderName
        commitMessageInput.value = "Upload $folderName via GitPusher"
        scanFolder(uri)
    }

    fun scanFolder(uri: Uri? = _selectedFolderUri.value) {
        if (uri == null) return

        viewModelScope.launch {
            _isScanningFiles.value = true
            val result = repository.scanDirectory(uri, ignoreBuildDirs.value)
            result.onSuccess { files ->
                _scannedFiles.value = files
            }.onFailure {
                _scannedFiles.value = emptyList()
            }
            _isScanningFiles.value = false
        }
    }

    fun createRepository(
        name: String,
        description: String?,
        isPrivate: Boolean,
        autoInit: Boolean,
        onSuccess: (GitHubRepo) -> Unit
    ) {
        val token = (authState.value as? AuthState.Authenticated)?.token ?: tokenInput.value
        if (token.isBlank() || name.isBlank()) return

        viewModelScope.launch {
            _isCreatingRepo.value = true
            val result = repository.createRepository(token, name.trim(), description, isPrivate, autoInit)
            result.onSuccess { repo ->
                selectRepo(repo)
                loadUserRepositories(token)
                onSuccess(repo)
            }
            _isCreatingRepo.value = false
        }
    }

    fun pushFiles() {
        val token = (authState.value as? AuthState.Authenticated)?.token ?: tokenInput.value.trim()
        val owner = ownerInput.value.trim()
        val repo = repoNameInput.value.trim()
        val branch = branchInput.value.trim().ifBlank { "main" }
        val folderName = _selectedFolderName.value ?: "Selected Folder"
        val commitMsg = commitMessageInput.value.trim().ifBlank { "Upload files via GitPusher" }
        val targetSubfolder = targetSubfolderInput.value.trim()
        val files = _scannedFiles.value

        if (token.isBlank()) {
            _pushUiState.value = PushUiState.Error("GitHub token is missing. Please connect your account.")
            return
        }
        if (owner.isBlank() || repo.isBlank()) {
            _pushUiState.value = PushUiState.Error("Please specify both username/owner and repository name.")
            return
        }
        if (files.isEmpty()) {
            _pushUiState.value = PushUiState.Error("No files found to push. Please select a folder first.")
            return
        }

        viewModelScope.launch {
            _pushUiState.value = PushUiState.Pushing("Preparing to push ${files.size} files...")
            repository.pushFilesToRepo(
                token = token,
                owner = owner,
                repo = repo,
                branch = branch,
                folderName = folderName,
                commitMessage = commitMsg,
                targetSubfolder = targetSubfolder,
                files = files
            ) { progress ->
                when (progress) {
                    is PushProgress.Scanning -> {
                        _pushUiState.value = PushUiState.Pushing(
                            message = progress.message,
                            progressPercent = 0.05f
                        )
                    }
                    is PushProgress.UploadingBlob -> {
                        _pushUiState.value = PushUiState.Pushing(
                            message = "Uploading blob ${progress.index}/${progress.total}",
                            currentIndex = progress.index,
                            totalFiles = progress.total,
                            progressPercent = progress.percent,
                            currentFile = progress.currentFile
                        )
                    }
                    is PushProgress.CreatingTree -> {
                        _pushUiState.value = PushUiState.Pushing(
                            message = progress.message,
                            progressPercent = 0.75f
                        )
                    }
                    is PushProgress.CreatingCommit -> {
                        _pushUiState.value = PushUiState.Pushing(
                            message = progress.message,
                            progressPercent = 0.88f
                        )
                    }
                    is PushProgress.UpdatingBranch -> {
                        _pushUiState.value = PushUiState.Pushing(
                            message = progress.message,
                            progressPercent = 0.95f
                        )
                    }
                    is PushProgress.Completed -> {
                        _pushUiState.value = PushUiState.Success(
                            commitSha = progress.commitSha,
                            commitUrl = progress.commitUrl,
                            fileCount = progress.fileCount
                        )
                    }
                    is PushProgress.Failed -> {
                        _pushUiState.value = PushUiState.Error(progress.error)
                    }
                }
            }
        }
    }

    fun dismissPushResult() {
        _pushUiState.value = PushUiState.Idle
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }
}
