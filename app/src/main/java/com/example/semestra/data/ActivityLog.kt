package com.example.semestra.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activity_logs",
    indices = [Index(value = ["user_id"]), Index(value = ["created_at"])]
)
data class ActivityLog(
    @PrimaryKey
    @ColumnInfo(name = "log_id")
    val logId: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "details")
    val details: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
