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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.example.semestra.R
import com.example.semestra.data.AppDatabase
import com.example.semestra.data.EventStatus
import com.example.semestra.data.ExamEvent
import com.example.semestra.data.SessionStore
import com.example.semestra.data.Syllabus
import com.example.semestra.logic.PdfUploadValidator
import com.example.semestra.logic.ActivityLogWriter
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class UploadActivity : AppCompatActivity() {

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var statusText: TextView
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var uploadButton: MaterialButton

    private val pickSyllabus = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                runCatching {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            processSyllabi(uris)
        } else Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)
        title = getString(R.string.upload_title)

        statusText = findViewById(R.id.textUploadStatus)
        progressIndicator = findViewById(R.id.progressUpload)
        uploadButton = findViewById(R.id.buttonPickPdf)

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
        val userId = SessionStore.getUserId(this)
        if (userId.isNullOrBlank()) {
            Toast.makeText(this, R.string.session_required, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        for (uri in uris) {
            if (!PdfUploadValidator.isSupportedSyllabus(this, uri)) {
                statusText.text = getString(R.string.error_not_supported_file)
                Toast.makeText(this, R.string.error_not_supported_file, Toast.LENGTH_LONG).show()
                return
            }
            if (PdfUploadValidator.exceedsSizeLimit(this, uri)) {
                statusText.text = getString(R.string.error_pdf_too_large)
                Toast.makeText(this, R.string.error_pdf_too_large, Toast.LENGTH_LONG).show()
                return
            }
        }

        setLoading(true)
        statusText.text = getString(R.string.upload_extracting_nlp)

        activityScope.launch {
            delay(3000)
            setLoading(false)
            showSectionDialog(userId, uris)
        }
    }

    private fun setLoading(loading: Boolean) {
        progressIndicator.visibility = if (loading) View.VISIBLE else View.GONE
        uploadButton.isEnabled = !loading
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }

    private fun showSectionDialog(userId: String, uris: List<Uri>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_verify_sections, null)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerSections)
        val sections = listOf("001", "002", "003")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sections)

        AlertDialog.Builder(this)
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
        activityScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val db = AppDatabase.getInstance(applicationContext)
                    db.examEventDao().deleteForUser(userId)

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
                    db.examEventDao().insertAll(events)
                    ActivityLogWriter.write(
                        applicationContext,
                        userId,
                        getString(R.string.activity_log_upload_title),
                        getString(R.string.activity_log_upload_details, events.size)
                    )
                    events.size
                }
            }
            setLoading(false)
            result.fold(
                onSuccess = { count ->
                    statusText.text = getString(R.string.upload_seed_complete, count)
                    startActivity(Intent(this@UploadActivity, ScheduleActivity::class.java))
                    finish()
                },
                onFailure = { err ->
                    statusText.text = getString(R.string.upload_error, err.localizedMessage.orEmpty())
                }
            )
        }
    }

    private fun demoSeedEvents(userId: String): List<ExamEvent> {
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
        val rows = listOf(
            // CSE 3302
            Triple("CSE 3302", "Jan 13, 2026", "Course Overview & Intro to Programming Languages"),
            Triple("CSE 3302", "Jan 20, 2026", "Language Design Criteria"),
            Triple("CSE 3302", "Jan 27, 2026", "Functional Programming"),
            Triple("CSE 3302", "Feb 3, 2026", "Logic Programming"),
            Triple("CSE 3302", "Feb 10, 2026", "Object-Oriented Programming"),
            Triple("CSE 3302", "Feb 17, 2026", "Syntax"),
            Triple("CSE 3302", "Feb 24, 2026", "Basic Semantics"),
            Triple("CSE 3302", "Mar 3, 2026", "Midterm Exam"),
            Triple("CSE 3302", "Mar 17, 2026", "Data Types Control"),
            Triple("CSE 3302", "Mar 24, 2026", "Control Structures I"),
            Triple("CSE 3302", "Mar 31, 2026", "Control Structures II"),
            Triple("CSE 3302", "Apr 7, 2026", "Abstract Data Types and Modules"),
            Triple("CSE 3302", "Apr 14, 2026", "Abstract Data Types and Modules (Continued)"),
            Triple("CSE 3302", "Apr 21, 2026", "Review and Advanced Topics"),
            Triple("CSE 3302", "May 5, 2026", "Final Exam"),
            // CSE 3314
            Triple("CSE 3314", "Jan 13, 2026", "Syllabus & Course Admin"),
            Triple("CSE 3314", "Jan 15, 2026", "Ideal Employee"),
            Triple("CSE 3314", "Jan 20, 2026", "Business Communication"),
            Triple("CSE 3314", "Jan 22, 2026", "Lecture - Chapter 1 (Assignment 1 Due)"),
            Triple("CSE 3314", "Jan 29, 2026", "Lecture - Chapter 2"),
            Triple("CSE 3314", "Feb 3, 2026", "Quiz 1"),
            Triple("CSE 3314", "Feb 10, 2026", "Guest Speaker #1 (Assignment 2 Due)"),
            Triple("CSE 3314", "Feb 19, 2026", "Lecture - Chapter 4"),
            Triple("CSE 3314", "Mar 3, 2026", "Quiz 2"),
            Triple("CSE 3314", "Mar 5, 2026", "Exam #1 (Assignment 3 Due)"),
            Triple("CSE 3314", "Mar 17, 2026", "Lecture - Chapter 5"),
            Triple("CSE 3314", "Mar 24, 2026", "Lecture - Chapter 6 (Assignment 4 Due)"),
            Triple("CSE 3314", "Mar 31, 2026", "Quiz 3"),
            Triple("CSE 3314", "Apr 7, 2026", "Guest Speaker #2"),
            Triple("CSE 3314", "Apr 9, 2026", "Lecture - Chapter 8 (Assignment 5 Due)"),
            Triple("CSE 3314", "Apr 16, 2026", "Communicating in a Teamworking Environment"),
            Triple("CSE 3314", "Apr 21, 2026", "Entrepreneurship"),
            Triple("CSE 3314", "Apr 23, 2026", "Resume Writing and Interviews"),
            Triple("CSE 3314", "Apr 28, 2026", "Quiz 4"),
            Triple("CSE 3314", "Apr 30, 2026", "Final Exam"),
            // CSE 3310
            Triple("CSE 3310", "Jan 13, 2026", "Course Introduction"),
            Triple("CSE 3310", "Jan 20, 2026", "Software Processes & Project Management"),
            Triple("CSE 3310", "Jan 27, 2026", "Discuss Term Project & Teams"),
            Triple("CSE 3310", "Feb 3, 2026", "Intro to UML"),
            Triple("CSE 3310", "Feb 10, 2026", "Requirements Engineering"),
            Triple("CSE 3310", "Feb 19, 2026", "Increment I Delivery Due (UML Document)"),
            Triple("CSE 3310", "Feb 24, 2026", "Software Testing"),
            Triple("CSE 3310", "Mar 3, 2026", "Midterm Exam"),
            Triple("CSE 3310", "Mar 5, 2026", "Android General Training"),
            Triple("CSE 3310", "Mar 17, 2026", "Software Evolution"),
            Triple("CSE 3310", "Mar 26, 2026", "Increment 2 Delivery Due (SRA Document)"),
            Triple("CSE 3310", "Mar 31, 2026", "Distributed SE & Cloud Computing"),
            Triple("CSE 3310", "Apr 7, 2026", "Agile Software Dev & Managing People"),
            Triple("CSE 3310", "Apr 16, 2026", "Increment 3 Delivery Due (Test Plan & Peer Reviews)"),
            Triple("CSE 3310", "Apr 21, 2026", "Team Presentations Begin"),
            Triple("CSE 3310", "Apr 28, 2026", "Increment 4 Delivery Due (Final Project Binder)"),
            Triple("CSE 3310", "May 5, 2026", "Final Exam"),
            // Quiet CSE 3315
            Triple("CSE 3315", "Jan 14, 2026", "Software Requirements Fundamentals"),
            Triple("CSE 3315", "Jan 21, 2026", "User Stories and Use Cases"),
            Triple("CSE 3315", "Feb 4, 2026", "Requirements Elicitation Workshop"),
            Triple("CSE 3315", "Feb 18, 2026", "Stakeholder Analysis"),
            Triple("CSE 3315", "Mar 4, 2026", "Midterm Assessment"),
            Triple("CSE 3315", "Mar 25, 2026", "Requirements Validation"),
            Triple("CSE 3315", "Apr 15, 2026", "Section Presentation and Peer Review"),
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
}
