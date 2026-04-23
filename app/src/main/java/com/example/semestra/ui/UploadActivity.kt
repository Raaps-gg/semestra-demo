package com.example.semestra.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.semestra.R
import com.example.semestra.data.AppDatabase
import com.example.semestra.data.EventStatus
import com.example.semestra.data.ExamEvent
import com.example.semestra.data.SessionStore
import com.example.semestra.data.Syllabus
import com.example.semestra.logic.PdfTextExtractor
import com.example.semestra.logic.PdfUploadValidator
import com.example.semestra.logic.SyllabusParser
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class UploadActivity : AppCompatActivity() {

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val parser = SyllabusParser()

    private lateinit var statusText: TextView
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var uploadButton: MaterialButton

    private val pickPdf = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) processPdf(uri)
        else Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)
        title = getString(R.string.upload_title)

        statusText = findViewById(R.id.textUploadStatus)
        progressIndicator = findViewById(R.id.progressUpload)
        uploadButton = findViewById(R.id.buttonPickPdf)

        uploadButton.setOnClickListener {
            pickPdf.launch("application/pdf")
        }
    }

    private fun processPdf(uri: Uri) {
        val userId = SessionStore.getUserId(this)
        if (userId.isNullOrBlank()) {
            Toast.makeText(this, R.string.session_required, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (!PdfUploadValidator.isPdf(this, uri)) {
            statusText.text = getString(R.string.error_not_pdf)
            Toast.makeText(this, R.string.error_not_pdf, Toast.LENGTH_LONG).show()
            return
        }
        if (PdfUploadValidator.exceedsSizeLimit(this, uri)) {
            statusText.text = getString(R.string.error_pdf_too_large)
            Toast.makeText(this, R.string.error_pdf_too_large, Toast.LENGTH_LONG).show()
            return
        }

        setLoading(true)
        statusText.text = getString(R.string.upload_reading_pdf)

        activityScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val syllabusId = UUID.randomUUID().toString()
                    val db = AppDatabase.getInstance(applicationContext)
                    db.syllabusDao().insert(
                        Syllabus(
                            syllabusId = syllabusId,
                            userId = userId,
                            filePath = uri.toString(),
                            uploadDate = System.currentTimeMillis()
                        )
                    )

                    val rawText = PdfTextExtractor.extractText(applicationContext, uri)
                    val parseResult = parser.parseText(rawText)
                    val events = parseResult.events.map { parsed ->
                        ExamEvent(
                            eventId = UUID.randomUUID().toString(),
                            userId = userId,
                            syllabusId = syllabusId,
                            className = parsed.className,
                            examTitle = parsed.examTitle,
                            eventDate = parsed.eventDate,
                            synced = false,
                            status = EventStatus.RAW,
                            needsReview = parsed.needsReview
                        )
                    }
                    if (events.isNotEmpty()) {
                        db.examEventDao().insertAll(events)
                    }
                    syllabusId to events.size
                }
            }

            setLoading(false)
            result.fold(
                onSuccess = { (syllabusId, _) ->
                    statusText.text = getString(R.string.upload_parse_complete)
                    startActivity(
                        Intent(this, EventReviewActivity::class.java).apply {
                            putExtra(EventReviewActivity.EXTRA_SYLLABUS_ID, syllabusId)
                        }
                    )
                    finish()
                },
                onFailure = { err ->
                    statusText.text = getString(R.string.upload_error, err.localizedMessage.orEmpty())
                }
            )
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
}
