package com.example.semestra

import com.example.semestra.logic.SyllabusParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun parseText_extractsScheduleRowFromDatePrefix() {
        val text = """
            CSE 3310 Section 001
            3/3/2026 Midterm Until the end of Software Testing + UML
            3/5/2026 Android general training
        """.trimIndent()

        val result = parser.parseText(text)
        assertTrue(result.events.size >= 2)
        assertTrue(result.events.any { it.examTitle.contains("Midterm", ignoreCase = true) })
        assertTrue(result.events.any { it.examTitle.contains("Android general training", ignoreCase = true) })
        assertTrue(result.events.any { it.className.contains("CSE 3310") })
    }

    @Test
    fun parseText_ignoresInstitutionalPolicyFluffRows() {
        val text = """
            Institutional Information
            Drop Policy (Last Day to Drop a Class is 10/25)
            https://resources.uta.edu/provost/course-related-info/institutional-policies.php
            Emergency Phone Numbers
        """.trimIndent()

        val result = parser.parseText(text)
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun parseText_marksSpringBreakAsClassSession() {
        val text = """
            CSE 3310 Section 001
            3/10/2026 Spring Break
        """.trimIndent()

        val result = parser.parseText(text)
        assertEquals(1, result.events.size)
        assertTrue(result.events.first().examTitle.contains("Spring Break", ignoreCase = true))
        assertFalse(result.events.first().needsReview)
    }
}
