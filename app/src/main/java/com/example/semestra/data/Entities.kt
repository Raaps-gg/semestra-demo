package com.example.semestra.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exam_events")
data class ExamEvent(
    @PrimaryKey val eventID: String, // [cite: 171]
    val className: String,           // [cite: 171]
    val examTitle: String,           // [cite: 171]
    val eventDate: Long,             // Stored as Long for SQLite compatibility [cite: 172]
    val synced: Boolean = false      // [cite: 173]
)