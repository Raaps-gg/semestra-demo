package com.example.semestra.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ActivityLog)

    @Query(
        """
        SELECT * FROM activity_logs
        WHERE user_id = :userId
        ORDER BY created_at DESC
        LIMIT :limit
        """
    )
    fun observeRecentForUser(userId: String, limit: Int = 8): Flow<List<ActivityLog>>

    @Query("DELETE FROM activity_logs WHERE log_id = :logId")
    suspend fun deleteById(logId: String): Int
}
