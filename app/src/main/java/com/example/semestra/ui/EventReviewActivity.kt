package com.example.semestra.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.semestra.R
import com.example.semestra.data.AppDatabase
import com.example.semestra.data.EventStatus
import com.example.semestra.data.ExamEvent
import com.example.semestra.data.SessionStore
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventReviewActivity : AppCompatActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var syllabusId: String
    private var reviewFinishedConfirmed: Boolean = false

    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: View
    private lateinit var adapter: EventReviewAdapter

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (reviewFinishedConfirmed) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            } else {
                showDiscardDialog()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_review)
        title = getString(R.string.review_title)

        syllabusId = intent.getStringExtra(EXTRA_SYLLABUS_ID).orEmpty()
        if (syllabusId.isBlank()) {
            finish()
            return
        }

        onBackPressedDispatcher.addCallback(this, backCallback)

        recycler = findViewById(R.id.recyclerReviewEvents)
        emptyView = findViewById(R.id.textReviewEmpty)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = EventReviewAdapter(
            onEdit = { showEditDialog(it) },
            onDelete = { deleteEvent(it) }
        )
        recycler.adapter = adapter

        findViewById<MaterialButton>(R.id.buttonConfirmAll).setOnClickListener { confirmAll() }

        loadEvents()
    }

    private fun loadEvents() {
        scope.launch {
            val events = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(applicationContext).examEventDao().getRawBySyllabus(syllabusId)
            }
            adapter.submitList(events)
            val showEmpty = events.isEmpty()
            emptyView.visibility = if (showEmpty) View.VISIBLE else View.GONE
            recycler.visibility = if (showEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun deleteEvent(event: ExamEvent) {
        scope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.getInstance(applicationContext).examEventDao().delete(event)
            }
            loadEvents()
        }
    }

    private fun showEditDialog(event: ExamEvent) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_event, null)
        val titleField = view.findViewById<TextInputEditText>(R.id.editEventTitle)
        val dateField = view.findViewById<TextInputEditText>(R.id.editEventDate)
        titleField.setText(event.examTitle)
        dateField.setText(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(event.eventDate)))

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit)
            .setView(view)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.registration_cancel) { d, _ -> d.dismiss() }
            .show()
            .also { dialog ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val newTitle = titleField.text?.toString()?.trim().orEmpty()
                    val dateStr = dateField.text?.toString()?.trim().orEmpty()
                    if (newTitle.isBlank()) {
                        titleField.error = getString(R.string.edit_title_required)
                        return@setOnClickListener
                    }
                    val epoch = try {
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)?.time
                    } catch (_: ParseException) {
                        null
                    }
                    if (epoch == null) {
                        dateField.error = getString(R.string.edit_date_invalid)
                        return@setOnClickListener
                    }
                    dialog.dismiss()
                    scope.launch {
                        val updated = event.copy(examTitle = newTitle, eventDate = epoch)
                        withContext(Dispatchers.IO) {
                            AppDatabase.getInstance(applicationContext).examEventDao().update(updated)
                        }
                        loadEvents()
                    }
                }
            }
    }

    private fun confirmAll() {
        val userId = SessionStore.getUserId(this)
        if (userId.isNullOrBlank()) {
            Toast.makeText(this, R.string.session_required, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        scope.launch {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getInstance(applicationContext)
                db.examEventDao().markRawSavedForSyllabus(
                    syllabusId,
                    EventStatus.RAW,
                    EventStatus.SAVED
                )
            }
            reviewFinishedConfirmed = true
            backCallback.isEnabled = false
            // #region agent log
            runCatching {
                java.io.File("/Users/vanthiang/Semestra/.cursor/debug-751471.log").appendText(
                    """{"sessionId":"751471","runId":"pre-fix","hypothesisId":"H3","location":"EventReviewActivity.kt:confirmAll","message":"Navigating to schedule","data":{"syllabusIdBlank":${syllabusId.isBlank()}},"timestamp":${System.currentTimeMillis()}}""" + "\n"
                )
            }
            // #endregion
            startActivity(Intent(this@EventReviewActivity, ScheduleActivity::class.java))
            finish()
        }
    }

    private fun showDiscardDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.discard_review_title)
            .setMessage(R.string.discard_review_message)
            .setNegativeButton(R.string.stay, null)
            .setPositiveButton(R.string.discard) { _, _ ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        AppDatabase.getInstance(applicationContext).examEventDao()
                            .deleteRawForSyllabus(syllabusId, EventStatus.RAW)
                    }
                    reviewFinishedConfirmed = true
                    backCallback.isEnabled = false
                    finish()
                }
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        const val EXTRA_SYLLABUS_ID = "extra_syllabus_id"
    }
}
