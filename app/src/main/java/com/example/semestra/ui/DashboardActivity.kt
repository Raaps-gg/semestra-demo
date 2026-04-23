package com.example.semestra.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.google.android.material.snackbar.Snackbar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.semestra.R
import com.example.semestra.data.AppDatabase
import com.example.semestra.data.SessionStore
import com.example.semestra.logic.ActivityLogWriter
import com.example.semestra.logic.CalendarServices
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import android.widget.TextView
import java.util.Locale
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import java.util.Calendar

class DashboardActivity : AppCompatActivity() {

    private lateinit var calendarServices: CalendarServices
    private lateinit var simulateButton: MaterialButton
    private lateinit var todayDate: TextView
    private lateinit var todaySummary: TextView
    private lateinit var activityRecycler: RecyclerView
    private lateinit var activityEmpty: TextView
    private lateinit var activityAdapter: ActivityLogAdapter
    private lateinit var activityFilterChips: ChipGroup
    private var activityFilterMode: ActivityFilterMode = ActivityFilterMode.TODAY
    private var allActivityLogs: List<com.example.semestra.data.ActivityLog> = emptyList()

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            runCalendarSync(account)
        } catch (e: ApiException) {
            showSyncMessage(getString(R.string.calendar_sync_auth_denied))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        title = getString(R.string.dashboard_title)
        calendarServices = CalendarServices(applicationContext)
        simulateButton = findViewById(R.id.buttonSimulateNotifications)
        todayDate = findViewById(R.id.textTodayDate)
        todaySummary = findViewById(R.id.textTodaySummary)
        activityRecycler = findViewById(R.id.recyclerActivityLog)
        activityEmpty = findViewById(R.id.textActivityLogEmpty)
        activityFilterChips = findViewById(R.id.chipGroupActivityFilter)
        activityRecycler.layoutManager = LinearLayoutManager(this)
        activityAdapter = ActivityLogAdapter()
        activityAdapter.setOnClickListener { log -> openLogContext(log.title) }
        activityRecycler.adapter = activityAdapter
        attachActivitySwipeActions()

        findViewById<MaterialButton>(R.id.buttonUploadSyllabus).setOnClickListener {
            startActivity(Intent(this, UploadActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.buttonViewSchedule).setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.buttonMySyllabi).setOnClickListener {
            startActivity(Intent(this, MySyllabiActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.buttonClassDirectory).setOnClickListener {
            startActivity(Intent(this, ClassDirectoryActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.buttonSyncGoogleCalendar).setOnClickListener {
            beginGoogleCalendarFlow()
        }
        simulateButton.setOnClickListener { simulateUpcomingNotifications() }

        // Hidden utility for test builds: long press subtitle to reveal simulator.
        findViewById<View>(R.id.textDashboardSubtitle).setOnLongClickListener {
            if (simulateButton.visibility != View.VISIBLE) {
                simulateButton.visibility = View.VISIBLE
                showSyncMessage(getString(R.string.dashboard_simulator_enabled))
            }
            true
        }
        activityFilterChips.setOnCheckedChangeListener { _, checkedId ->
            activityFilterMode = when (checkedId) {
                R.id.chipActivityWeek -> ActivityFilterMode.THIS_WEEK
                R.id.chipActivityAll -> ActivityFilterMode.ALL
                else -> ActivityFilterMode.TODAY
            }
            renderActivityLogs()
        }
        refreshTodayPanel()
        observeActivityTimeline()
    }

    override fun onResume() {
        super.onResume()
        refreshTodayPanel()
    }

    private fun beginGoogleCalendarFlow() {
        val userId = SessionStore.getUserId(this)
        if (userId.isNullOrBlank()) {
            showSyncMessage(getString(R.string.session_required))
            return
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(CalendarScopes.CALENDAR_EVENTS))
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    private fun runCalendarSync(account: GoogleSignInAccount) {
        val userId = SessionStore.getUserId(this) ?: return
        val root = findViewById<View>(android.R.id.content)
        lifecycleScope.launch {
            when (
                val result = withContext(Dispatchers.IO) {
                    calendarServices.syncSavedEvents(
                        account,
                        AppDatabase.getInstance(applicationContext),
                        userId
                    )
                }
            ) {
                is CalendarServices.SyncResult.Success -> {
                    withContext(Dispatchers.IO) {
                        ActivityLogWriter.write(
                            applicationContext,
                            userId,
                            getString(R.string.activity_log_sync_title),
                            getString(R.string.activity_log_sync_details, result.pushedCount)
                        )
                    }
                    Snackbar.make(
                        root,
                        getString(R.string.calendar_sync_success, result.pushedCount),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                is CalendarServices.SyncResult.Failure -> {
                    val bar = Snackbar.make(root, result.message, Snackbar.LENGTH_LONG)
                    bar.setAction(R.string.retry) {
                        beginGoogleCalendarFlow()
                    }
                    bar.show()
                }
            }
        }
    }

    private fun showSyncMessage(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }

    private fun simulateUpcomingNotifications() {
        val userId = SessionStore.getUserId(this)
        if (userId.isNullOrBlank()) {
            showSyncMessage(getString(R.string.session_required))
            return
        }
        lifecycleScope.launch {
            val message = withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                val weekLater = now + 7L * 24L * 60L * 60L * 1000L
                val events = AppDatabase.getInstance(applicationContext)
                    .examEventDao()
                    .getSavedInRange(userId, now, weekLater)
                if (events.isEmpty()) {
                    getString(R.string.dashboard_simulator_none)
                } else {
                    events.take(5).joinToString("\n") { event ->
                        val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(event.eventDate))
                        getString(R.string.dashboard_simulator_item, event.examTitle, date)
                    }
                }
            }
            MaterialAlertDialogBuilder(this@DashboardActivity)
                .setTitle(R.string.dashboard_simulator_title)
                .setMessage(getString(R.string.dashboard_simulator_message, message))
                .setPositiveButton(R.string.ok, null)
                .show()
        }
    }

    private fun refreshTodayPanel() {
        todayDate.text = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault()).format(Date())
        val userId = SessionStore.getUserId(this) ?: return
        lifecycleScope.launch {
            val panelText = withContext(Dispatchers.IO) {
                val db = AppDatabase.getInstance(applicationContext)
                val now = System.currentTimeMillis()
                val dayWindow = DisplayGenerator.windowFor(DisplayGenerator.RangeMode.DAILY, now)
                val todayEvents = db.examEventDao().getSavedInRange(userId, dayWindow.startInclusive, dayWindow.endExclusive)
                val latest = db.syllabusDao().getLatestForUser(userId)
                val eventPart = if (todayEvents.isEmpty()) {
                    getString(R.string.dashboard_today_no_class)
                } else {
                    todayEvents.take(3).joinToString(" | ") { e ->
                        getString(R.string.dashboard_today_event_item, e.className, e.examTitle)
                    }
                }
                val notePart = latest?.let {
                    val meeting = it.meetingInfo.orEmpty()
                    val grading = it.gradingSummary.orEmpty().take(120)
                    listOf(meeting, grading).filter { s -> s.isNotBlank() }.joinToString(" ")
                }.orEmpty()
                if (notePart.isBlank()) eventPart else "$eventPart\n$notePart"
            }
            todaySummary.text = panelText
        }
    }

    private fun observeActivityTimeline() {
        val userId = SessionStore.getUserId(this) ?: return
        val db = AppDatabase.getInstance(applicationContext)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.activityLogDao().observeRecentForUser(userId).collect { logs ->
                    allActivityLogs = logs
                    renderActivityLogs()
                }
            }
        }
    }

    private fun renderActivityLogs() {
        val filtered = filterLogs(allActivityLogs, activityFilterMode)
        activityAdapter.submitList(filtered)
        val showEmpty = filtered.isEmpty()
        activityEmpty.visibility = if (showEmpty) View.VISIBLE else View.GONE
        activityRecycler.visibility = if (showEmpty) View.GONE else View.VISIBLE
    }

    private fun filterLogs(
        logs: List<com.example.semestra.data.ActivityLog>,
        mode: ActivityFilterMode
    ): List<com.example.semestra.data.ActivityLog> {
        if (mode == ActivityFilterMode.ALL) return logs
        val now = Calendar.getInstance()
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (mode == ActivityFilterMode.THIS_WEEK) {
                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            }
        }.timeInMillis
        val end = if (mode == ActivityFilterMode.TODAY) {
            start + 24L * 60L * 60L * 1000L
        } else {
            now.timeInMillis + 1L
        }
        return logs.filter { it.createdAt in start until end }
    }

    private fun openLogContext(title: String) {
        val next = when {
            title.contains("sync", ignoreCase = true) -> Intent(this, ScheduleActivity::class.java)
            title.contains("parsed", ignoreCase = true) -> Intent(this, MySyllabiActivity::class.java)
            title.contains("removed", ignoreCase = true) -> Intent(this, ScheduleActivity::class.java)
            else -> Intent(this, ClassDirectoryActivity::class.java)
        }
        startActivity(next)
    }

    private enum class ActivityFilterMode { TODAY, THIS_WEEK, ALL }

    private fun attachActivitySwipeActions() {
        val helper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val item = activityAdapter.getItemAt(viewHolder.bindingAdapterPosition) ?: return
                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        openLogContext(item.title)
                        activityAdapter.notifyItemChanged(viewHolder.bindingAdapterPosition)
                    }
                    ItemTouchHelper.RIGHT -> {
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                AppDatabase.getInstance(applicationContext).activityLogDao().deleteById(item.logId)
                            }
                            Snackbar.make(
                                findViewById(android.R.id.content),
                                getString(R.string.activity_log_dismissed),
                                Snackbar.LENGTH_LONG
                            ).setAction(R.string.retry) {
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        AppDatabase.getInstance(applicationContext).activityLogDao().insert(item)
                                    }
                                }
                            }.show()
                        }
                    }
                }
            }
        })
        helper.attachToRecyclerView(activityRecycler)
    }
}
