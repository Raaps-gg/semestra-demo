package com.example.semestra.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CourseProfile>)

    @Update
    suspend fun update(item: CourseProfile)

    @Query("SELECT * FROM course_profiles WHERE user_id = :userId ORDER BY course_name ASC")
    fun observeForUser(userId: String): Flow<List<CourseProfile>>

    @Query("DELETE FROM course_profiles WHERE user_id = :userId")
    suspend fun deleteForUser(userId: String): Int
}
