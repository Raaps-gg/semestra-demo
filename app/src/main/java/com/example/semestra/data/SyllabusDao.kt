package com.example.semestra.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyllabusDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(syllabus: Syllabus)

    @Query("SELECT * FROM syllabi WHERE syllabus_id = :syllabusId LIMIT 1")
    suspend fun getById(syllabusId: String): Syllabus?

    @Query("SELECT * FROM syllabi WHERE user_id = :userId ORDER BY upload_date DESC")
    fun observeForUser(userId: String): Flow<List<Syllabus>>

    @Query("SELECT * FROM syllabi WHERE user_id = :userId ORDER BY upload_date DESC LIMIT 1")
    suspend fun getLatestForUser(userId: String): Syllabus?
}
