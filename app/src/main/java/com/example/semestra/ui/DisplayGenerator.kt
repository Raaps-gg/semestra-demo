package com.example.semestra.ui

import com.example.semestra.data.ExamEvent
import java.util.Calendar
import java.util.TimeZone

/**
 * Pure helpers for schedule filtering and window math (used by [ScheduleActivity]).
 */
object DisplayGenerator {

    data class TimeWindow(val startInclusive: Long, val endExclusive: Long)

    enum class RangeMode { DAILY, WEEKLY, MONTHLY }

    fun windowFor(mode: RangeMode, anchorUtcMillis: Long, zone: TimeZone = TimeZone.getDefault()): TimeWindow {
        val cal = Calendar.getInstance(zone).apply { timeInMillis = anchorUtcMillis }
        return when (mode) {
            RangeMode.DAILY -> {
                stripToDayStart(cal)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_MONTH, 1)
                TimeWindow(start, cal.timeInMillis)
            }
            RangeMode.WEEKLY -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                stripToDayStart(cal)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_MONTH, 7)
                TimeWindow(start, cal.timeInMillis)
            }
            RangeMode.MONTHLY -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                stripToDayStart(cal)
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                TimeWindow(start, cal.timeInMillis)
            }
        }
    }

    fun eventsInWindow(events: List<ExamEvent>, window: TimeWindow): List<ExamEvent> {
        return events
            .filter { it.eventDate >= window.startInclusive && it.eventDate < window.endExclusive }
            .sortedBy { it.eventDate }
    }

    private fun stripToDayStart(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }
}
