package com.example.semestra.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.semestra.R
import com.example.semestra.data.AppDatabase
import com.example.semestra.data.ExamEvent
import com.example.semestra.data.SessionStore
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.CalendarView
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

class HomeCalendarFragment : Fragment() {

    private lateinit var shellViewModel: MainShellViewModel

    private lateinit var calendarView: CalendarView
    private lateinit var monthTitle: TextView
    private lateinit var prevMonthButton: MaterialButton
    private lateinit var nextMonthButton: MaterialButton
    private lateinit var upcomingRecycler: RecyclerView
    private lateinit var eventsRecycler: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var adapter: ScheduleEventAdapter
    private lateinit var upcomingAdapter: ScheduleEventAdapter

    private var selectedDate: LocalDate = LocalDate.now()
    private var classFilter: String? = null
    private var allEvents: List<ExamEvent> = emptyList()
    private var eventsByDate: Map<LocalDate, List<ExamEvent>> = emptyMap()
    private var currentMonth: YearMonth = YearMonth.now()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_home_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        calendarView = view.findViewById(R.id.monthCalendarView)
        monthTitle = view.findViewById(R.id.textCurrentMonth)
        prevMonthButton = view.findViewById(R.id.buttonPrevMonth)
        nextMonthButton = view.findViewById(R.id.buttonNextMonth)
        upcomingRecycler = view.findViewById(R.id.recyclerUpcomingEvents)
        shellViewModel = ViewModelProvider(requireActivity())[MainShellViewModel::class.java]
        eventsRecycler = view.findViewById(R.id.recyclerHomeEvents)
        emptyView = view.findViewById(R.id.textHomeEmpty)
        adapter = ScheduleEventAdapter { event ->
            EventDetailsBottomSheet.newInstance(event.eventId)
                .show(parentFragmentManager, "event_details")
        }
        upcomingAdapter = ScheduleEventAdapter { event ->
            EventDetailsBottomSheet.newInstance(event.eventId)
                .show(parentFragmentManager, "event_details")
        }
        upcomingRecycler.layoutManager = LinearLayoutManager(requireContext())
        upcomingRecycler.adapter = upcomingAdapter
        eventsRecycler.layoutManager = LinearLayoutManager(requireContext())
        eventsRecycler.adapter = adapter
        val sheet = view.findViewById<View>(R.id.layoutHomeBody)
        BottomSheetBehavior.from(sheet).apply {
            isHideable = true
            skipCollapsed = false
            state = BottomSheetBehavior.STATE_COLLAPSED
        }

        setupCalendar()
        observeFilter()
        observeEvents()
    }

    private fun setupCalendar() {
        currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(6)
        val endMonth = currentMonth.plusMonths(8)
        val firstDayOfWeek = daysOfWeek(firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek)
        calendarView.setup(startMonth, endMonth, firstDayOfWeek.first())
        calendarView.scrollToMonth(currentMonth)
        monthTitle.text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        calendarView.monthScrollListener = { month ->
            currentMonth = month.yearMonth
            monthTitle.text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        }
        prevMonthButton.setOnClickListener { calendarView.smoothScrollToMonth(currentMonth.minusMonths(1)) }
        nextMonthButton.setOnClickListener { calendarView.smoothScrollToMonth(currentMonth.plusMonths(1)) }

        class DayContainer(view: View) : ViewContainer(view) {
            val textDay: TextView = view.findViewById(R.id.textDayNumber)
            val dotContainer: LinearLayout = view.findViewById(R.id.dotContainer)
            lateinit var day: CalendarDay

            init {
                view.setOnClickListener {
                    if (day.position == DayPosition.MonthDate) {
                        selectedDate = day.date
                        calendarView.notifyCalendarChanged()
                        renderSelectedDay()
                    }
                }
            }
        }

        calendarView.dayBinder = object : MonthDayBinder<DayContainer> {
            override fun create(view: View): DayContainer = DayContainer(view)

            override fun bind(container: DayContainer, data: CalendarDay) {
                container.day = data
                val inMonth = data.position == DayPosition.MonthDate
                container.textDay.text = data.date.dayOfMonth.toString()
                container.textDay.alpha = if (inMonth) 1f else 0.35f
                when {
                    data.date == selectedDate -> {
                        container.textDay.setBackgroundResource(R.drawable.bg_calendar_day_selected)
                        container.textDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.notion_text_primary))
                    }
                    data.date == LocalDate.now() -> {
                        container.textDay.setBackgroundResource(R.drawable.bg_calendar_today_outline)
                        container.textDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.two_tone_on_dark))
                    }
                    else -> {
                        container.textDay.setBackgroundResource(0)
                        container.textDay.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                if (inMonth) R.color.two_tone_on_dark else R.color.two_tone_on_dark_secondary
                            )
                        )
                    }
                }
                bindDots(container.dotContainer, eventsByDate[data.date].orEmpty())
            }
        }
    }

    private fun bindDots(container: LinearLayout, events: List<ExamEvent>) {
        container.removeAllViews()
        val colors = events.mapNotNull { dotColorFor(it) }.distinct()
        colors.forEach { colorRes ->
            val dot = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dp(6), dp(6)).apply { marginEnd = dp(3) }
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_calendar_dot)
                background?.setTint(ContextCompat.getColor(requireContext(), colorRes))
            }
            container.addView(dot)
        }
    }

    private fun observeFilter() {
        shellViewModel.classFilter.observe(viewLifecycleOwner) { filter ->
            classFilter = filter
            mapAndRender()
        }
    }

    private fun observeEvents() {
        val userId = SessionStore.getUserId(requireContext()) ?: return
        val db = AppDatabase.getInstance(requireContext().applicationContext)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.examEventDao().observeSavedForUser(userId).collect { events ->
                    allEvents = events
                    mapAndRender()
                }
            }
        }
    }

    private fun mapAndRender() {
        val filtered = allEvents.filter { classFilter.isNullOrBlank() || it.className == classFilter }
        eventsByDate = filtered.groupBy { millisToDate(it.eventDate) }
        if (filtered.isNotEmpty() && eventsByDate[selectedDate].isNullOrEmpty()) {
            selectedDate = millisToDate(filtered.minOf { it.eventDate })
            val selectedMonth = YearMonth.of(selectedDate.year, selectedDate.month)
            calendarView.scrollToMonth(selectedMonth)
        }
        calendarView.notifyCalendarChanged()
        renderSelectedDay()
    }

    private fun renderSelectedDay() {
        val dayEvents = eventsByDate[selectedDate].orEmpty().sortedBy { it.eventDate }
        val upcoming = allEvents
            .filter { millisToDate(it.eventDate) >= LocalDate.now() }
            .sortedBy { it.eventDate }
            .take(5)
        adapter.submitList(dayEvents)
        upcomingAdapter.submitList(upcoming)
        emptyView.visibility = if (dayEvents.isEmpty()) View.VISIBLE else View.GONE
        eventsRecycler.visibility = if (dayEvents.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun dotColorFor(event: ExamEvent): Int? {
        val title = event.examTitle.lowercase(Locale.US)
        val type = event.eventType.lowercase(Locale.US)
        return when {
            "exam" in title || "midterm" in title || "final" in title || type == "exam" -> R.color.event_exam_stripe
            "quiz" in title || type == "quiz" -> R.color.event_quiz_stripe
            "assignment" in title || "delivery" in title || "due" in title || type == "assignment" -> R.color.event_assignment_stripe
            "lecture" in title || type == "lecture" -> R.color.event_lecture_stripe
            else -> null
        }
    }

    private fun millisToDate(millis: Long): LocalDate {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
