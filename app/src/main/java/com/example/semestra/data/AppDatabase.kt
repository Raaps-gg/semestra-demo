package com.example.semestra.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [User::class, ExamEvent::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    // You would add DAOs here for saveUser(), saveEvent(), etc. [cite: 161, 164]
}