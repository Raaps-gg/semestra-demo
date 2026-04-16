package com.example.semestra.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val userID: String, // [cite: 152]
    val name: String,              // [cite: 153]
    val email: String,             // [cite: 154]
    val passwordHash: String       // [cite: 155]
)