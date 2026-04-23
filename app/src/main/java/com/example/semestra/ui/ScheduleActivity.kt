package com.example.semestra.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.semestra.R
import com.example.semestra.data.AppDatabase
import com.example.semestra.data.ExamEvent
import com.example.semestra.data.SessionStore
import com.example.semestra.logic.ActivityLogWriter
import com.example.semestra.logic.CalendarServices
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.DateFormat
import java.util.Date

class ScheduleActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: View
    private lateinit var chipGroup: ChipGroup
    private lateinit var adapter: ScheduleEventAdapter
    private lateinit var calendarServices: CalendarServices

    private var mode: DisplayGenerator.RangeMode = DisplayGenerator.RangeMode.WEEKLY
    private var lastSaved: List<ExamEvent> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)
        title = getString(R.string.schedule_title)

        val userId = SessionStore.getUserId(this)
        if (userId.isNullOrBlank()) {
            Toast.makeText(this, R.string.session_required, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        recycler = findViewById(R.id.recyclerSchedule)
        emptyView = findViewById(R.id.textScheduleEmpty)
        chipGroup = findViewById(R.id.chipGroupRange)
        calendarServices = CalendarServices(applicationContext)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = ScheduleEventAdapter { showDetail(it) }
        recycler.adapter = adapter

        chipGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == View.NO_ID) return@setOnCheckedChangeListener
            mode = when (checkedId) {
                R.id.chipDaily -> DisplayGenerator.RangeMode.DAILY
                R.id.chipMonthly -> DisplayGenerator.RangeMode.MONTHLY
                else -> DisplayGenerator.RangeMode.WEEKLY
            }
            applyFilter()
        }

        val db = AppDatabase.getInstance(applicationContext)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.examEventDao().observeSavedForUser(userId).collect { allSaved ->
                    lastSaved = allSaved
                    applyFilter()
                }
            }
        }
    }

    private fun applyFilter() {
        val window = DisplayGenerator.windowFor(mode, System.currentTimeMillis())
        val visible = DisplayGenerator.eventsInWindow(lastSaved, window)
        adapter.submitList(visible)
        val empty = visible.isEmpty()
        emptyView.visibility = if (empty) View.VISIBLE else View.GONE
        recycler.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private fun showDetail(event: ExamEvent) {
        val dateStr = DateFormat.getDateInstance(DateFormat.FULL).format(Date(event.eventDate))
        val syncStr = getString(if (event.synced) R.string.synced_label else R.string.not_synced_label)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.event_detail_title)
            .setMessage(
                getString(
                    R.string.event_detail_body,
                    event.examTitle,
                    event.className,
                    dateStr,
                    syncStr
                )
            )
            .setNeutralButton(R.string.event_delete) { _, _ -> showDeleteDialog(event) }
            .setPositiveButton(R.string.ok) { d, _ -> d.dismiss() }
            .show()
    }

    private fun showDeleteDialog(event: ExamEvent) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.event_delete_confirm_title)
            .setMessage(R.string.event_delete_confirm_message)
            .setNegativeButton(R.string.registration_cancel, null)
            .setPositiveButton(R.string.event_delete) { _, _ ->
                deleteEventFromSchedule(event)
            }
            .show()
    }

    private fun deleteEventFromSchedule(event: ExamEvent) {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            if (event.synced && !event.googleEventId.isNullOrBlank()) {
                val account = GoogleSignIn.getLastSignedInAccount(this@ScheduleActivity)
                if (account != null) {
                    when (
                        val result = calendarServices.deleteFromCalendarIfSynced(
                            account = account,
                            database = db,
                            eventId = event.eventId,
                            googleEventId = event.googleEventId
                        )
                    ) {
                        is CalendarServices.SyncResult.Success -> {
                            withContext(Dispatchers.IO) {
                                val userId = SessionStore.getUserId(this@ScheduleActivity)
                                if (!userId.isNullOrBlank()) {
                                    ActivityLogWriter.write(
                                        applicationContext,
                                        userId,
                                        getString(R.string.activity_log_delete_title),
                                        getString(R.string.activity_log_delete_details, event.examTitle)
                                    )
                                }
                            }
                            Toast.makeText(
                                this@ScheduleActivity,
                                R.string.event_delete_success,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        is CalendarServices.SyncResult.Failure -> {
                            Toast.makeText(
                                this@ScheduleActivity,
                                result.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    return@launch
                }
                withContext(Dispatchers.IO) {
                    db.examEventDao().delete(event)
                    ActivityLogWriter.write(
                        applicationContext,
                        event.userId,
                        getString(R.string.activity_log_delete_title),
                        getString(R.string.activity_log_delete_details, event.examTitle)
                    )
                }
                Toast.makeText(
                    this@ScheduleActivity,
                    R.string.event_delete_local_only,
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            withContext(Dispatchers.IO) {
                db.examEventDao().delete(event)
                ActivityLogWriter.write(
                    applicationContext,
                    event.userId,
                    getString(R.string.activity_log_delete_title),
                    getString(R.string.activity_log_delete_details, event.examTitle)
                )
            }
            Toast.makeText(this@ScheduleActivity, R.string.event_delete_success, Toast.LENGTH_SHORT)
                .show()
        }
    }
}
