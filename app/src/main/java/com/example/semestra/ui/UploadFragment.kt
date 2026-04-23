package com.example.semestra.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.semestra.R
import com.example.semestra.data.AppDatabase
import com.example.semestra.data.CourseProfile
import com.example.semestra.data.EventStatus
import com.example.semestra.data.ExamEvent
import com.example.semestra.data.SessionStore
import com.example.semestra.data.Syllabus
import com.example.semestra.logic.ActivityLogWriter
import com.example.semestra.logic.PdfUploadValidator
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class UploadFragment : Fragment(R.layout.fragment_upload) {

    private lateinit var statusText: TextView
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var uploadButton: MaterialButton

    private val pickSyllabus = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                runCatching {
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            processSyllabi(uris)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        statusText = view.findViewById(R.id.textUploadStatus)
        progressIndicator = view.findViewById(R.id.progressUpload)
        uploadButton = view.findViewById(R.id.buttonPickPdf)

        uploadButton.setOnClickListener {
            pickSyllabus.launch(
                arrayOf(
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                )
            )
        }
    }

    private fun processSyllabi(uris: List<Uri>) {
        val userId = SessionStore.getUserId(requireContext()) ?: return
        for (uri in uris) {
            if (!PdfUploadValidator.isSupportedSyllabus(requireContext(), uri)) {
                Toast.makeText(requireContext(), R.string.error_not_supported_file, Toast.LENGTH_LONG).show()
                return
            }
            if (PdfUploadValidator.exceedsSizeLimit(requireContext(), uri)) {
                Toast.makeText(requireContext(), R.string.error_pdf_too_large, Toast.LENGTH_LONG).show()
                return
            }
        }
        setLoading(true)
        statusText.text = getString(R.string.upload_extracting_nlp)
        viewLifecycleOwner.lifecycleScope.launch {
            delay(3000)
            setLoading(false)
            showSectionDialog(userId, uris)
        }
    }

    private fun showSectionDialog(userId: String, uris: List<Uri>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_verify_sections, null)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerSections)
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("001", "002", "003"))
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.upload_verify_section_title)
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton(R.string.upload_confirm) { _, _ ->
                val section = spinner.selectedItem?.toString().orEmpty().ifBlank { "001" }
                seedDemoData(userId, uris, section)
            }
            .show()
    }

    private fun seedDemoData(userId: String, uris: List<Uri>, section: String) {
        setLoading(true)
        statusText.text = getString(R.string.upload_building_demo_calendar)
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val db = AppDatabase.getInstance(requireContext().applicationContext)
                    db.examEventDao().deleteForUser(userId)
                    db.courseProfileDao().deleteForUser(userId)
                    uris.forEach { uri ->
                        db.syllabusDao().insert(
                            Syllabus(
                                syllabusId = UUID.randomUUID().toString(),
                                userId = userId,
                                filePath = uri.toString(),
                                uploadDate = System.currentTimeMillis(),
                                courseSection = "CSE 3315 Section $section"
                            )
                        )
                    }
                    val events = demoSeedEvents(userId)
                    val courses = demoProfiles(userId)
                    db.examEventDao().insertAll(events)
                    db.courseProfileDao().insertAll(courses)
                    ActivityLogWriter.write(
                        requireContext().applicationContext,
                        userId,
                        getString(R.string.activity_log_upload_title),
                        getString(R.string.activity_log_upload_details, events.size)
                    )
                    events.size
                }
            }
            setLoading(false)
            result.onSuccess {
                statusText.text = getString(R.string.upload_seed_complete, it)
                SessionStore.markDemoParsed(requireContext(), true)
                (activity as? MainShellActivity)?.openHomeWithClassFilter(null)
            }.onFailure {
                statusText.text = getString(R.string.upload_error, it.localizedMessage.orEmpty())
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressIndicator.visibility = if (loading) View.VISIBLE else View.GONE
        uploadButton.isEnabled = !loading
    }

    private fun demoSeedEvents(userId: String): List<ExamEvent> {
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
        val rows = listOf(
            Triple("CSE 3302", "Mar 3, 2026", "Midterm Exam"),
            Triple("CSE 3302", "May 5, 2026", "Final Exam"),
            Triple("CSE 3314", "Feb 3, 2026", "Quiz 1"),
            Triple("CSE 3314", "Mar 5, 2026", "Exam #1 (Assignment 3 Due)"),
            Triple("CSE 3310", "Feb 19, 2026", "Increment I Delivery Due (UML Document)"),
            Triple("CSE 3310", "May 5, 2026", "Final Exam"),
            Triple("CSE 3315", "Mar 4, 2026", "Midterm Assessment"),
            Triple("CSE 3315", "May 6, 2026", "Final Exam")
        )
        return rows.map { (className, date, title) ->
            val millis = dateFormat.parse(date)?.time ?: System.currentTimeMillis()
            ExamEvent(
                eventId = UUID.randomUUID().toString(),
                userId = userId,
                syllabusId = null,
                className = className,
                examTitle = title,
                eventDate = millis,
                location = defaultLocationFor(className),
                startTime = defaultStartFor(className),
                endTime = defaultEndFor(className),
                eventType = eventTypeFromTitle(title),
                synced = false,
                status = EventStatus.SAVED,
                needsReview = false
            )
        }
    }

    private fun defaultLocationFor(className: String): String = when (className) {
        "CSE 3315" -> "NH 202"
        "CSE 3302" -> "NH 109"
        "CSE 3314" -> "NH 203"
        "CSE 3310" -> "SWSH 221"
        else -> "TBD"
    }

    private fun eventTypeFromTitle(title: String): String {
        val s = title.lowercase(Locale.US)
        return when {
            "exam" in s || "midterm" in s || "final" in s -> "Exam"
            "quiz" in s -> "Quiz"
            "assignment" in s || "delivery" in s || "due" in s -> "Assignment"
            else -> "Lecture"
        }
    }

    private fun defaultStartFor(className: String): String = when (className) {
        "CSE 3315" -> "11:00 AM"
        "CSE 3302" -> "12:30 PM"
        "CSE 3314" -> "03:30 PM"
        "CSE 3310" -> "02:00 PM"
        else -> "09:00 AM"
    }

    private fun defaultEndFor(className: String): String = when (className) {
        "CSE 3315" -> "12:20 PM"
        "CSE 3302" -> "01:50 PM"
        "CSE 3314" -> "04:50 PM"
        "CSE 3310" -> "03:20 PM"
        else -> "10:00 AM"
    }

    private fun demoProfiles(userId: String): List<CourseProfile> = listOf(
        CourseProfile(
            "${userId}_CSE3315_002", userId, "CSE 3315", "002",
            "Tu/Th 11:00 AM", "NH 202", "barasch@exchange.uta.edu",
            "Hanani Pankaj", "HW 5%, Quizzes 15%, Exams 50%, Final 30%"
        ),
        CourseProfile(
            "${userId}_CSE3302_001", userId, "CSE 3302", "001",
            "Tu/Th 12:30 PM", "NH 109", "jiandong.wang@uta.edu",
            "cxh1126@mavs.uta.edu", "Labs 25%, HW 35%, Midterm 20%, Final 20%"
        ),
        CourseProfile(
            "${userId}_CSE3314_004", userId, "CSE 3314", "004",
            "Tu/Th 3:30 PM", "NH 203", "nomaan.mufti@uta.edu",
            "mohamed.mohamed4@mavs.uta.edu", "Exams 100pts, Quizzes 30pts, 5 Assignments"
        ),
        CourseProfile(
            "${userId}_CSE3310_001", userId, "CSE 3310", "001",
            "Tu/Th 2:00 PM", "SWSH 221", "khalili@uta.edu",
            "lxs5171@mavs.uta.edu", "Mid-term 25%, Final 25%, Term Project 50%"
        )
    )
}
