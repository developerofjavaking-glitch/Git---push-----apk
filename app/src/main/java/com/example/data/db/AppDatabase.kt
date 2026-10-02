package com.example.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface PushHistoryDao {
    @Query("SELECT * FROM push_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<PushHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: PushHistoryEntity): Long

    @Query("DELETE FROM push_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM push_history")
    suspend fun clearAll()
}

@Dao
interface UserAuthDao {
    @Query("SELECT * FROM user_auth WHERE id = 1 LIMIT 1")
    fun getSavedAuth(): Flow<UserAuthEntity?>

    @Query("SELECT * FROM user_auth WHERE id = 1 LIMIT 1")
    suspend fun getSavedAuthOnce(): UserAuthEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAuth(auth: UserAuthEntity)

    @Query("DELETE FROM user_auth WHERE id = 1")
    suspend fun clearAuth()
}

@Database(
    entities = [PushHistoryEntity::class, UserAuthEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pushHistoryDao(): PushHistoryDao
    abstract fun userAuthDao(): UserAuthDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "git_pusher.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
