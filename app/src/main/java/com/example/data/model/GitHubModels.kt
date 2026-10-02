package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubUser(
    val login: String,
    val id: Long,
    @Json(name = "avatar_url") val avatarUrl: String?,
    val name: String?,
    val bio: String?,
    @Json(name = "html_url") val htmlUrl: String,
    @Json(name = "public_repos") val publicRepos: Int = 0,
    @Json(name = "total_private_repos") val totalPrivateRepos: Int? = 0
)

@JsonClass(generateAdapter = true)
data class GitHubRepo(
    val id: Long,
    val name: String,
    @Json(name = "full_name") val fullName: String,
    val private: Boolean,
    @Json(name = "html_url") val htmlUrl: String,
    val description: String?,
    val fork: Boolean = false,
    @Json(name = "default_branch") val defaultBranch: String? = "main",
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "stargazers_count") val stargazersCount: Int = 0,
    val language: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateRepoRequest(
    val name: String,
    val description: String? = null,
    val private: Boolean = false,
    @Json(name = "auto_init") val autoInit: Boolean = true
)

@JsonClass(generateAdapter = true)
data class GitBlobRequest(
    val content: String,
    val encoding: String = "base64"
)

@JsonClass(generateAdapter = true)
data class GitBlobResponse(
    val sha: String,
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class GitTreeEntry(
    val path: String,
    val mode: String = "100644",
    val type: String = "blob",
    val sha: String
)

@JsonClass(generateAdapter = true)
data class GitTreeRequest(
    @Json(name = "base_tree") val baseTree: String? = null,
    val tree: List<GitTreeEntry>
)

@JsonClass(generateAdapter = true)
data class GitTreeResponse(
    val sha: String,
    val url: String? = null,
    val truncated: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class GitCommitRequest(
    val message: String,
    val tree: String,
    val parents: List<String>
)

@JsonClass(generateAdapter = true)
data class GitCommitResponse(
    val sha: String,
    val url: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GitRefObject(
    val sha: String,
    val type: String? = null,
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class GitRefResponse(
    val ref: String,
    @Json(name = "node_id") val nodeId: String? = null,
    val url: String? = null,
    @Json(name = "object") val obj: GitRefObject
)

@JsonClass(generateAdapter = true)
data class UpdateRefRequest(
    val sha: String,
    val force: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CreateRefRequest(
    val ref: String,
    val sha: String
)

data class LocalScannedFile(
    val uriString: String,
    val relativePath: String,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String?
)
