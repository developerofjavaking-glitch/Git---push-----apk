package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "push_history")
data class PushHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val repoOwner: String,
    val repoName: String,
    val branch: String,
    val folderName: String,
    val commitMessage: String,
    val commitSha: String?,
    val commitUrl: String?,
    val fileCount: Int,
    val totalBytes: Long,
    val status: String, // "SUCCESS" or "FAILED"
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_auth")
data class UserAuthEntity(
    @PrimaryKey
    val id: Int = 1,
    val token: String,
    val username: String,
    val name: String? = null,
    val avatarUrl: String? = null,
    val savedAt: Long = System.currentTimeMillis()
)
