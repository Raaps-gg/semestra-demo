package com.example.semestra.logic

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ParsedExamEvent(
    val className: String,
    val examTitle: String,
    val eventDate: Long,
    val needsReview: Boolean
)

data class ParseResult(val events: List<ParsedExamEvent>)

class SyllabusParser {

    fun parseText(text: String): ParseResult {
        val events = mutableListOf<ParsedExamEvent>()
        val lines = text.lines()

        for ((index, line) in lines.withIndex()) {
            val examMatch = EXAM_LABEL_REGEX.find(line) ?: continue
            val searchWindow = lines.drop(index).take(4).joinToString(" ")
            val dateMatches = DATE_REGEX.findAll(searchWindow).toList()
            val dateMatch = dateMatches.firstOrNull() ?: continue
            val timeMatch = TIME_REGEX.find(searchWindow)

            val epochMs = parseToEpoch(dateMatch.value.trim(), timeMatch?.value?.trim()) ?: continue

            val className = extractClassName(line)
            val title = examMatch.value.replaceFirstChar { it.uppercase() }.trim()

            val multipleDates = dateMatches.size > 1
            val unknownCourse = className == "Unknown Course"
            val needsReview = multipleDates || unknownCourse

            events.add(
                ParsedExamEvent(
                    className = className,
                    examTitle = title,
                    eventDate = epochMs,
                    needsReview = needsReview
                )
            )
        }

        val distinct = events.distinctBy { it.eventDate to it.examTitle }
        return ParseResult(distinct)
    }

    private fun parseToEpoch(dateStr: String, timeStr: String?): Long? {
        val cleanDate = dateStr.replace(Regex("""(\d+)(?:st|nd|rd|th)"""), "$1").trim()
        val fullStr = if (timeStr != null) "$cleanDate $timeStr" else cleanDate

        val formatsToTry = if (timeStr != null) {
            DATE_FORMATS.flatMap { df ->
                listOf(
                    SimpleDateFormat("${df.toPattern()} h:mm a", Locale.US),
                    SimpleDateFormat("${df.toPattern()} ha", Locale.US),
                    SimpleDateFormat("${df.toPattern()} HH:mm", Locale.US),
                    df
                )
            }
        } else {
            DATE_FORMATS
        }

        for (fmt in formatsToTry) {
            try {
                fmt.isLenient = false
                val date = fmt.parse(fullStr) ?: fmt.parse(cleanDate) ?: continue
                val cal = Calendar.getInstance().apply { time = date }
                if (cal.get(Calendar.YEAR) < 2000) {
                    cal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR))
                }
                return cal.timeInMillis
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun extractClassName(line: String): String {
        return Regex("""[A-Z]{2,4}\s?\d{3,4}""").find(line)?.value ?: "Unknown Course"
    }

    companion object {
        private val EXAM_LABEL_REGEX = Regex(
            """((?:midterm|final|exam|quiz|test)\s*(?:\d+|i{1,3}|iv)?)\b""",
            RegexOption.IGNORE_CASE
        )
        private val DATE_REGEX = Regex(
            """(?:(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|""" +
                """Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\s+\d{1,2}(?:st|nd|rd|th)?,?\s+\d{4}|""" +
                """\d{1,2}[/\-]\d{1,2}[/\-]\d{2,4}|""" +
                """\d{4}-\d{2}-\d{2})""",
            RegexOption.IGNORE_CASE
        )
        private val TIME_REGEX = Regex(
            """\b(\d{1,2}:\d{2}\s*(?:AM|PM)|(\d{1,2}(?:AM|PM))|\d{2}:\d{2})\b""",
            RegexOption.IGNORE_CASE
        )
        private val DATE_FORMATS = listOf(
            SimpleDateFormat("MMMM d, yyyy", Locale.US),
            SimpleDateFormat("MMMM d yyyy", Locale.US),
            SimpleDateFormat("MMM d, yyyy", Locale.US),
            SimpleDateFormat("MMM d yyyy", Locale.US),
            SimpleDateFormat("MM/dd/yyyy", Locale.US),
            SimpleDateFormat("MM-dd-yyyy", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("MM/dd/yy", Locale.US),
            SimpleDateFormat("MMMM d", Locale.US),
            SimpleDateFormat("MMM d", Locale.US),
        )
    }
}
