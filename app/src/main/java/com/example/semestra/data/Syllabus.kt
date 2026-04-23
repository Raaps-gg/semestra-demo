package com.example.semestra.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "syllabi",
    indices = [Index(value = ["user_id"])],
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Syllabus(
    @PrimaryKey
    @ColumnInfo(name = "syllabus_id")
    val syllabusId: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "file_path")
    val filePath: String,
    @ColumnInfo(name = "upload_date")
    val uploadDate: Long,
    @ColumnInfo(name = "course_section")
    val courseSection: String? = null,
    @ColumnInfo(name = "instructor_name")
    val instructorName: String? = null,
    @ColumnInfo(name = "instructor_email")
    val instructorEmail: String? = null,
    @ColumnInfo(name = "ta_name")
    val taName: String? = null,
    @ColumnInfo(name = "ta_email")
    val taEmail: String? = null,
    @ColumnInfo(name = "meeting_info")
    val meetingInfo: String? = null,
    @ColumnInfo(name = "grading_summary")
    val gradingSummary: String? = null
)
