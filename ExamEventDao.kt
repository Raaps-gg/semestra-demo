package com.example.semestra.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExamEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<ExamEvent>)

    @Query("SELECT * FROM exam_events ORDER BY event_date ASC")
    suspend fun getAllEvents(): List<ExamEvent>

    @Query("DELETE FROM exam_events")
    suspend fun clearAll()
}