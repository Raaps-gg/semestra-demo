package com.example.semestra.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exam_events")
data class ExamEvent(
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: String,
    @ColumnInfo(name = "class_name")
    val className: String,
    @ColumnInfo(name = "exam_title")
    val examTitle: String,
    @ColumnInfo(name = "event_date")
    val eventDate: Long,
    @ColumnInfo(name = "synced")
    val synced: Boolean = false
)
