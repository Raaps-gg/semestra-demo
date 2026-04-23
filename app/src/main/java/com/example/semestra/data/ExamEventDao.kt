package com.example.semestra.data

import androidx.room.*  // <--- Ensure this is here
import com.example.semestra.data.ExamEvent

@Dao
interface ExamEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<ExamEvent>)

    @Query("SELECT * FROM exam_events ORDER BY event_date ASC")
    suspend fun getAllEvents(): List<ExamEvent>

    @Query("DELETE FROM exam_events")
    suspend fun clearAll()
}