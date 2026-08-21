package com.spendstreak.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {
    @Query("SELECT * FROM user_progress WHERE id = 0")
    fun observe(): Flow<UserProgress?>

    @Query("SELECT * FROM user_progress WHERE id = 0")
    suspend fun get(): UserProgress?

    @Upsert
    suspend fun upsert(progress: UserProgress)

    @Query("DELETE FROM user_progress")
    suspend fun deleteAll()
}
