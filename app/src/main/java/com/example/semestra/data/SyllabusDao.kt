package com.example.semestra.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyllabusDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(syllabus: Syllabus)

    @Query("SELECT * FROM syllabi WHERE syllabus_id = :syllabusId LIMIT 1")
    suspend fun getById(syllabusId: String): Syllabus?
}
