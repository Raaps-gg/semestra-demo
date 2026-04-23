package com.example.semestra

import com.example.semestra.logic.SyllabusParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyllabusParserTest {

    private val parser = SyllabusParser()

    @Test
    fun parseText_findsExamAndDate() {
        val text = """
            CS 101 Midterm Exam on October 10, 2026 at 2:00 PM
        """.trimIndent()
        val result = parser.parseText(text)
        assertEquals(1, result.events.size)
        assertTrue(result.events[0].examTitle.contains("Midterm", ignoreCase = true))
    }

    @Test
    fun parseText_returnsEmptyWhenNoDates() {
        val text = "This syllabus has no assessments listed anywhere."
        val result = parser.parseText(text)
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun parseText_marksUnknownCourseForReview() {
        val text = "The final exam will be on November 2, 2026."
        val result = parser.parseText(text)
        assertEquals(1, result.events.size)
        assertTrue(result.events[0].needsReview)
    }
}
