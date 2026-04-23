package com.example.semestra.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Version 8 adds course grading scale metadata for expanded class cards.
 * Version 7 adds event start/end time for timeline calendar rendering.
 * Version 6 adds course profiles and event location/type fields.
 * Version 5 adds persistent daily activity log timeline entries.
 * Version 4 adds syllabus metadata fields for dashboard and class directory UX.
 * Version 3 adds persisted Google Calendar event IDs for delete support.
 * Version 2 adds syllabi, event ownership, RAW/SAVED lifecycle, and review flags.
 * Uses destructive migration during active development; replace with incremental
 * migrations before shipping persistent user data.
 */
@Database(
    entities = [User::class, ExamEvent::class, Syllabus::class, ActivityLog::class, CourseProfile::class],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun examEventDao(): ExamEventDao
    abstract fun syllabusDao(): SyllabusDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun courseProfileDao(): CourseProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }

        private const val DATABASE_NAME = "AppDatabase"
    }
}
