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
        val defaultCourse = extractDefaultCourse(lines)

        for ((index, line) in lines.withIndex()) {
            val examMatch = EXAM_LABEL_REGEX.find(line) ?: continue
            val searchWindow = lines.drop(index).take(4).joinToString(" ")
            val dateMatches = DATE_REGEX.findAll(searchWindow).toList()
            val dateMatch = dateMatches.firstOrNull() ?: continue
            val timeMatch = TIME_REGEX.find(searchWindow)

            val epochMs = parseToEpoch(dateMatch.value.trim(), timeMatch?.value?.trim()) ?: continue

            val className = extractClassName(line, defaultCourse)
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

        // Parse schedule-like rows (date + topic/assignment text), common in DOCX/PDF tables.
        for ((index, rawLine) in lines.withIndex()) {
            val line = rawLine.trim()
            val prefix = ROW_DATE_PREFIX.find(line) ?: continue
            val dateText = prefix.value.trim()
            val eventDate = parseToEpoch(dateText, null) ?: continue

            val tail = line.removePrefix(prefix.value).trim(' ', '-', ':', '|', '\t')
            val contextTail = if (tail.isNotBlank()) tail else {
                lines.drop(index + 1).firstOrNull { it.trim().isNotBlank() }?.trim().orEmpty()
            }
            if (!looksLikeScheduleRow(contextTail)) continue

            val title = buildSessionTitle(contextTail)
            val className = extractClassName(line, defaultCourse)
            val unknownCourse = className == "Unknown Course"
            val needsReview = unknownCourse

            events.add(
                ParsedExamEvent(
                    className = className,
                    examTitle = title,
                    eventDate = eventDate,
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

    private fun extractClassName(line: String, defaultCourse: String?): String {
        return Regex("""[A-Z]{2,4}\s?\d{3,4}""").find(line)?.value
            ?: defaultCourse
            ?: "Unknown Course"
    }

    private fun extractDefaultCourse(lines: List<String>): String? {
        return lines
            .asSequence()
            .map { it.trim() }
            .firstOrNull { it.contains(Regex("""\b[A-Z]{2,4}\s?\d{3,4}\b""")) }
            ?.let { Regex("""\b[A-Z]{2,4}\s?\d{3,4}\b""").find(it)?.value }
    }

    private fun looksLikeScheduleRow(content: String): Boolean {
        if (content.isBlank()) return false
        val lower = content.lowercase(Locale.US)
        val blacklist = listOf(
            "institutional information",
            "additional information",
            "drop policy",
            "emergency",
            "http://",
            "https://",
            "catalog",
            "grade grievances"
        )
        if (blacklist.any { lower.contains(it) }) return false
        val scheduleKeywords = listOf(
            "topic", "chapter", "assignment", "project", "midterm", "final",
            "quiz", "discussion", "training", "presentation", "class", "spring break",
            "lab", "test", "review", "requirements", "design", "modeling"
        )
        return scheduleKeywords.any { lower.contains(it) } || content.length > 18
    }

    private fun buildSessionTitle(content: String): String {
        val condensed = content.replace(Regex("""\s+"""), " ").trim()
        if (condensed.isBlank()) return "Class session"
        val clean = condensed.take(100)
        return if (clean.contains("no class", ignoreCase = true)) {
            clean.replaceFirstChar { it.uppercase() }
        } else {
            "Class: $clean"
        }
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
        private val ROW_DATE_PREFIX = Regex(
            """^\s*(\d{1,2}/\d{1,2}/\d{2,4}|(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{1,2},?\s+\d{2,4})""",
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
