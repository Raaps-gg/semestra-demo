package com.example.semestra.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "course_profiles",
    indices = [Index(value = ["user_id"])]
)
data class CourseProfile(
    @PrimaryKey
    @ColumnInfo(name = "profile_id")
    val profileId: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "course_name")
    val courseName: String,
    @ColumnInfo(name = "section")
    val section: String,
    @ColumnInfo(name = "meeting_times")
    val meetingTimes: String,
    @ColumnInfo(name = "location")
    val location: String,
    @ColumnInfo(name = "prof_email")
    val profEmail: String,
    @ColumnInfo(name = "ta_email")
    val taEmail: String,
    @ColumnInfo(name = "grading_scale")
    val gradingScale: String = "",
    @ColumnInfo(name = "notes")
    val notes: String = ""
)
