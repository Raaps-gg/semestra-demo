package com.example.semestra.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exam_events",
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["syllabus_id"]),
        Index(value = ["event_date"]),
        Index(value = ["status"])
    ]
)
data class ExamEvent(
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "syllabus_id")
    val syllabusId: String?,
    @ColumnInfo(name = "class_name")
    val className: String,
    @ColumnInfo(name = "exam_title")
    val examTitle: String,
    @ColumnInfo(name = "event_date")
    val eventDate: Long,
    @ColumnInfo(name = "synced")
    val synced: Boolean = false,
    @ColumnInfo(name = "google_event_id")
    val googleEventId: String? = null,
    @ColumnInfo(name = "status")
    val status: EventStatus = EventStatus.SAVED,
    @ColumnInfo(name = "needs_review")
    val needsReview: Boolean = false
)
