package com.example.semestra.data

import androidx.room.TypeConverter

enum class EventStatus {
    RAW,
    SAVED
}

class Converters {
    @TypeConverter
    fun fromEventStatus(value: EventStatus): String = value.name

    @TypeConverter
    fun toEventStatus(value: String): EventStatus = EventStatus.valueOf(value)
}
