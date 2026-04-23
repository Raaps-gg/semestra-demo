package com.example.semestra.ui

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.semestra.R
import com.example.semestra.data.AppDatabase
import com.example.semestra.data.ExamEvent
import com.example.semestra.data.SessionStore
import com.example.semestra.logic.ActivityLogWriter
import com.example.semestra.logic.CalendarServices
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.graphics.drawable.GradientDrawable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ScheduleActivity : AppCompatActivity() {

    private lateinit var weekStripContainer: LinearLayout
    private lateinit var hourLabelsContainer: LinearLayout
    private lateinit var dayEventsCanvas: FrameLayout
    private lateinit var emptyView: View
    private lateinit var calendarServices: CalendarServices

    private var classFilter: String? = null
    private var importantOnly: Boolean = false
    private var lastSaved: List<ExamEvent> = emptyList()
    private var selectedDayStart: Long = startOfDay(System.currentTimeMillis())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)
        title = getString(R.string.schedule_title)
        classFilter = intent.getStringExtra("class_filter")
        importantOnly = intent.getBooleanExtra("important_only", false)

        val userId = SessionStore.getUserId(this)
        if (userId.isNullOrBlank()) {
            Toast.makeText(this, R.string.session_required, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        weekStripContainer = findViewById(R.id.weekStripContainer)
        hourLabelsContainer = findViewById(R.id.hourLabelsContainer)
        dayEventsCanvas = findViewById(R.id.dayEventsCanvas)
        emptyView = findViewById(R.id.textScheduleEmpty)
        calendarServices = CalendarServices(applicationContext)
        buildWeekStrip()

        val db = AppDatabase.getInstance(applicationContext)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.examEventDao().observeSavedForUser(userId).collect { allSaved ->
                    lastSaved = applyGlobalFilters(allSaved)
                    if (importantOnly && lastSaved.isNotEmpty()) {
                        val first = startOfDay(lastSaved.first().eventDate)
                        selectedDayStart = first
                        buildWeekStrip()
                    }
                    renderTimelineForSelectedDate()
                }
            }
        }
    }

    private fun applyGlobalFilters(source: List<ExamEvent>): List<ExamEvent> {
        return source.filter { event ->
            val classOk = classFilter.isNullOrBlank() || event.className.equals(classFilter, ignoreCase = true)
            val importantOk = !importantOnly || event.eventType == "Exam" || event.eventType == "Quiz" || event.eventType == "Assignment"
            classOk && importantOk
        }
    }

    private fun renderTimelineForSelectedDate() {
        val end = selectedDayStart + DAY_MS
        val visible = lastSaved
            .filter { it.eventDate >= selectedDayStart && it.eventDate < end }
            .sortedBy { parseMinutes(it.startTime) }

        hourLabelsContainer.removeAllViews()
        dayEventsCanvas.removeAllViews()
        val timelineHeight = dp((END_HOUR - START_HOUR) * HOUR_HEIGHT_DP)
        dayEventsCanvas.layoutParams = dayEventsCanvas.layoutParams.apply {
            height = timelineHeight
        }

        for (hour in START_HOUR..END_HOUR) {
            val hourLabel = TextView(this).apply {
                text = hourToLabel(hour)
                setTextColor(getColor(R.color.notion_text_secondary))
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(HOUR_HEIGHT_DP)
                )
            }
            hourLabelsContainer.addView(hourLabel)

            val top = dp((hour - START_HOUR) * HOUR_HEIGHT_DP)
            val line = View(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    1
                ).apply { topMargin = top }
                setBackgroundColor(getColor(R.color.notion_stroke))
            }
            dayEventsCanvas.addView(line)
        }

        visible.forEach { event -> dayEventsCanvas.addView(buildEventBlock(event)) }

        val empty = visible.isEmpty()
        emptyView.visibility = if (empty) View.VISIBLE else View.GONE
        hourLabelsContainer.visibility = if (empty) View.GONE else View.VISIBLE
        dayEventsCanvas.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private fun buildWeekStrip() {
        weekStripContainer.removeAllViews()
        val start = Calendar.getInstance().apply {
            timeInMillis = selectedDayStart
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        }
        repeat(7) { index ->
            val day = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, index) }
            val dayStart = startOfDay(day.timeInMillis)
            val label = SimpleDateFormat("EEE\nd", Locale.US).format(day.time)
            val button = MaterialButton(this).apply {
                text = label
                isAllCaps = false
                textSize = 12f
                setPadding(16, 8, 16, 8)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 12 }
                setOnClickListener {
                    selectedDayStart = dayStart
                    buildWeekStrip()
                    renderTimelineForSelectedDate()
                }
                val selected = dayStart == selectedDayStart
                setBackgroundColor(if (selected) 0xFFEFEFED.toInt() else 0x00000000)
                setTextColor(0xFF2F2F2F.toInt())
            }
            weekStripContainer.addView(button)
        }
    }

    private fun buildEventBlock(event: ExamEvent): View {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                durationToHeight(event.startTime, event.endTime)
            ).apply {
                topMargin = startOffset(event.startTime)
                marginStart = dp(8)
                marginEnd = dp(8)
            }
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(getColor(typeBackground(event.eventType)))
            }
        }
        val stripe = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(4), LinearLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(getColor(typeStripe(event.eventType)))
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 10, 12, 10)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }
        val titleView = TextView(this).apply {
            text = event.examTitle
            textSize = 14f
            setTextColor(getColor(R.color.notion_text_primary))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val locationView = TextView(this).apply {
            text = "${event.location.ifBlank { "TBD" }} • ${event.startTime}-${event.endTime}"
            textSize = 12f
            setTextColor(getColor(R.color.notion_text_secondary))
        }
        content.addView(titleView)
        content.addView(locationView)
        outer.addView(stripe)
        outer.addView(content)
        outer.setOnClickListener { showDetail(event) }
        return outer
    }

    private fun hourToLabel(hour: Int): String {
        val ampm = if (hour < 12) "AM" else "PM"
        val h = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "$h $ampm"
    }

    private fun parseHour(time: String): Int {
        return try {
            val date = SimpleDateFormat("hh:mm a", Locale.US).parse(time)
            Calendar.getInstance().apply { this.time = date ?: Date() }.get(Calendar.HOUR_OF_DAY)
        } catch (_: Exception) {
            9
        }
    }

    private fun parseMinutes(time: String): Int {
        return try {
            val date = SimpleDateFormat("hh:mm a", Locale.US).parse(time)
            val cal = Calendar.getInstance().apply { this.time = date ?: Date() }
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        } catch (_: Exception) {
            9 * 60
        }
    }

    private fun durationToHeight(start: String, end: String): Int {
        val durationMinutes = (parseMinutes(end) - parseMinutes(start)).coerceAtLeast(30)
        return dp(durationMinutes * HOUR_HEIGHT_DP / 60)
    }

    private fun startOffset(start: String): Int {
        val relativeMinutes = (parseMinutes(start) - START_HOUR * 60).coerceAtLeast(0)
        return dp(relativeMinutes * HOUR_HEIGHT_DP / 60)
    }

    private fun typeBackground(type: String): Int = when (type) {
        "Exam" -> R.color.event_exam_bg
        "Quiz" -> R.color.event_quiz_bg
        "Assignment" -> R.color.event_assignment_bg
        else -> R.color.event_lecture_bg
    }

    private fun typeStripe(type: String): Int = when (type) {
        "Exam" -> R.color.event_exam_stripe
        "Quiz" -> R.color.event_quiz_stripe
        "Assignment" -> R.color.event_assignment_stripe
        else -> R.color.event_lecture_stripe
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun showDetail(event: ExamEvent) {
        EventDetailsBottomSheet.newInstance(event.eventId)
            .show(supportFragmentManager, "event_details")
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

    private fun startOfDay(time: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    private companion object {
        private const val DAY_MS = 24L * 60L * 60L * 1000L
        private const val START_HOUR = 7
        private const val END_HOUR = 17
        private const val HOUR_HEIGHT_DP = 72
    }
}
