package com.example.semestra.logic

data class SyllabusMetadata(
    val courseSection: String? = null,
    val instructorName: String? = null,
    val instructorEmail: String? = null,
    val taName: String? = null,
    val taEmail: String? = null,
    val meetingInfo: String? = null,
    val gradingSummary: String? = null
)

object SyllabusMetadataExtractor {
    fun extract(rawText: String): SyllabusMetadata {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val lower = lines.joinToString("\n").lowercase()

        val courseSection = lines.firstOrNull {
            it.contains(Regex("""\b([A-Z]{2,4}\s?\d{4})\b""")) && it.contains("section", true)
        } ?: lines.firstOrNull { it.contains(Regex("""\b([A-Z]{2,4}\s?\d{4})\b""")) }

        val instructorName = lines.firstOrNull {
            it.contains("Instructor Name", true)
        }?.substringAfter("Instructor Name", "")?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: lines.firstOrNull {
                it.matches(Regex("""(?i)dr\.\s+[A-Za-z][A-Za-z\s\.-]+"""))
            }

        val emailRegex = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
        val instructorEmail = emailRegex.find(lines.firstOrNull { it.contains("Email Address", true) }.orEmpty())
            ?.value ?: emailRegex.find(lines.firstOrNull { it.contains("instructor", true) && it.contains("@") }.orEmpty())?.value

        val taLine = lines.firstOrNull { it.contains("Teaching Assistant", true) || it.contains("GTA Name", true) }
            ?: lines.firstOrNull { it.contains("TA", true) && it.contains("@") }
        val taEmail = taLine?.let { emailRegex.find(it)?.value }
        val taName = taLine
            ?.replace(emailRegex, "")
            ?.replace("Teaching Assistant:", "", true)
            ?.replace("GTA Name", "", true)
            ?.replace(";", " ")
            ?.replace("\"", "")
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        val meetingInfo = lines.firstOrNull {
            it.contains("Time and Place of Class Meetings", true)
        }?.let { labelLine ->
            val idx = lines.indexOf(labelLine)
            lines.drop(idx).take(3).joinToString(" ").replace("Time and Place of Class Meetings", "", true).trim()
        } ?: lines.firstOrNull { it.contains(Regex("""(?i)(tu|th|tues|thurs|mon|wed|fri).*\d{1,2}:\d{2}""")) }

        val gradingSummary = when {
            lower.contains("grading information") || lower.contains("grade calculation") -> {
                lines.filter { it.contains(Regex("""(?i)(\d+%|grading|mid-?term|final|project|assignment)""")) }
                    .take(8)
                    .joinToString(" | ")
            }
            else -> null
        }?.takeIf { it.isNotBlank() }

        return SyllabusMetadata(
            courseSection = courseSection,
            instructorName = instructorName,
            instructorEmail = instructorEmail,
            taName = taName,
            taEmail = taEmail,
            meetingInfo = meetingInfo,
            gradingSummary = gradingSummary
        )
    }
}
