package com.example.semestra.logic

import android.content.Context
import com.example.semestra.data.ActivityLog
import com.example.semestra.data.AppDatabase
import java.util.UUID

object ActivityLogWriter {
    suspend fun write(context: Context, userId: String, title: String, details: String) {
        AppDatabase.getInstance(context).activityLogDao().insert(
            ActivityLog(
                logId = UUID.randomUUID().toString(),
                userId = userId,
                title = title,
                details = details,
                createdAt = System.currentTimeMillis()
            )
        )
    }
}
