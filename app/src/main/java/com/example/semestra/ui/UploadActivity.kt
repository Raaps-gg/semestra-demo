package com.example.semestra.ui

import android.net.Uri // MISSING PREVIOUSLY
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.*

import com.example.semestra.R
import com.example.semestra.data.AppDatabase
import com.example.semestra.logic.PdfTextExtractor
import com.example.semestra.logic.SyllabusParser // MISSING PREVIOUSLY

class UploadActivity : AppCompatActivity() {

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Ensure SyllabusParser.kt is fixed and doesn't have "Redeclaration" errors
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

        // Ensure "upload_title" exists in res/values/strings.xml
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
                    val rawText = PdfTextExtractor.extractText(applicationContext, uri)
                    val events = parser.parseText(rawText)

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
                        "✓ Found $count exam event(s)!"
                    else
                        "No exam dates detected."
                },
                onFailure = { err ->
                    statusText.text = "Error: ${err.localizedMessage}"
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