package com.example.semestra

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.semestra.data.AppDatabase
import com.example.semestra.logic.PdfTextExtractor
import com.example.semestra.logic.SyllabusParser
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UploadActivity : AppCompatActivity() {

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val parser = SyllabusParser()

    private lateinit var statusText: TextView
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var uploadButton: MaterialButton

    // Registers a file picker for PDFs
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

        statusText        = findViewById(R.id.textUploadStatus)
        progressIndicator = findViewById(R.id.progressUpload)
        uploadButton      = findViewById(R.id.buttonPickPdf)

        uploadButton.setOnClickListener {
            pickPdf.launch("application/pdf")
        }
    }

    private fun processPdf(uri: Uri) {
        setLoading(true)
        statusText.text = "Reading PDF…"

        activityScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    // 1. Extract text
                    val rawText = PdfTextExtractor.extractText(applicationContext, uri)
                    // 2. Parse into events
                    val events = parser.parseText(rawText)
                    // 3. Persist to Room
                    if (events.isNotEmpty()) {
                        val dao = AppDatabase.getInstance(applicationContext).examEventDao()
                        dao.insertAll(events)
                    }
                    events.size
                }
            }

            setLoading(false)
            result.fold(
                onSuccess = { count ->
                    statusText.text = if (count > 0)
                        "✓ Found $count exam event(s) — check your schedule!"
                    else
                        "No exam dates detected. Try a different syllabus."
                },
                onFailure = { err ->
                    statusText.text = "Error reading PDF: ${err.localizedMessage}"
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