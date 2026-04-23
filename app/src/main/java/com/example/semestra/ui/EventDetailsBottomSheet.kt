package com.example.semestra.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.semestra.R
import com.example.semestra.data.AppDatabase
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventDetailsBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_edit_schedule_event, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val eventId = requireArguments().getString(ARG_EVENT_ID) ?: return

        val title = view.findViewById<EditText>(R.id.editScheduleTitle)
        val date = view.findViewById<EditText>(R.id.editScheduleDate)
        val time = view.findViewById<EditText>(R.id.editScheduleTime)
        val location = view.findViewById<EditText>(R.id.editScheduleLocation)
        val type = view.findViewById<TextView>(R.id.textScheduleEventType)
        val className = view.findViewById<TextView>(R.id.textScheduleClassName)
        val cancel = view.findViewById<Button>(R.id.buttonCancelScheduleEdit)
        val edit = view.findViewById<Button>(R.id.buttonToggleScheduleEdit)
        val save = view.findViewById<Button>(R.id.buttonSaveScheduleEdit)

        setEditable(false, title, date, time, location)
        save.visibility = View.GONE
        cancel.setOnClickListener { dismiss() }

        lifecycleScope.launch {
            val event = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(requireContext().applicationContext).examEventDao().getById(eventId)
            } ?: return@launch

            className.text = getString(R.string.schedule_class_label, event.className)
            type.text = getString(R.string.schedule_type_label, event.eventType)
            title.setText(event.examTitle)
            date.setText(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(event.eventDate)))
            time.setText("${event.startTime} - ${event.endTime}")
            location.setText(event.location)

            edit.setOnClickListener {
                setEditable(true, title, date, time, location)
                save.visibility = View.VISIBLE
                edit.visibility = View.GONE
            }

            save.setOnClickListener {
                val parsedDate = try {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date.text?.toString().orEmpty())?.time
                } catch (_: ParseException) {
                    null
                }
                if (parsedDate == null) {
                    Toast.makeText(requireContext(), R.string.edit_date_invalid, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        val parts = time.text?.toString().orEmpty().split("-").map { it.trim() }
                        AppDatabase.getInstance(requireContext().applicationContext).examEventDao().update(
                            event.copy(
                                examTitle = title.text?.toString().orEmpty(),
                                eventDate = parsedDate,
                                location = location.text?.toString().orEmpty(),
                                startTime = parts.getOrNull(0) ?: event.startTime,
                                endTime = parts.getOrNull(1) ?: event.endTime
                            )
                        )
                    }
                    dismiss()
                }
            }
        }
    }

    private fun setEditable(enabled: Boolean, vararg fields: EditText) {
        fields.forEach {
            it.isEnabled = enabled
            it.isFocusable = enabled
            it.isFocusableInTouchMode = enabled
        }
    }

    companion object {
        private const val ARG_EVENT_ID = "event_id"

        fun newInstance(eventId: String): EventDetailsBottomSheet {
            return EventDetailsBottomSheet().apply {
                arguments = Bundle().apply { putString(ARG_EVENT_ID, eventId) }
            }
        }
    }
}
