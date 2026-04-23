package com.example.semestra.ui

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.semestra.R
import com.example.semestra.data.CourseProfile
import com.example.semestra.data.ExamEvent
import java.text.DateFormat
import java.util.Date

class ClassDirectoryAdapter(
    private val onSaveNotes: (CourseProfile, String) -> Unit
) : RecyclerView.Adapter<ClassDirectoryAdapter.Holder>() {
    private val items = mutableListOf<CourseProfile>()
    private val expandedProfileIds = mutableSetOf<String>()
    private val eventsByClass = mutableMapOf<String, List<ExamEvent>>()

    fun submitList(newItems: List<CourseProfile>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun submitClassEvents(events: List<ExamEvent>) {
        eventsByClass.clear()
        eventsByClass.putAll(events.groupBy { it.className })
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_class_directory, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val course = itemView.findViewById<TextView>(R.id.textDirectoryCourse)
        private val delete = itemView.findViewById<ImageButton>(R.id.buttonDeleteClass)
        private val section = itemView.findViewById<TextView>(R.id.textDirectorySection)
        private val meeting = itemView.findViewById<TextView>(R.id.textDirectoryMeeting)
        private val location = itemView.findViewById<TextView>(R.id.textDirectoryLocation)
        private val instructor = itemView.findViewById<TextView>(R.id.textDirectoryInstructor)
        private val ta = itemView.findViewById<TextView>(R.id.textDirectoryTA)
        private val grading = itemView.findViewById<TextView>(R.id.textDirectoryGrading)
        private val expandHint = itemView.findViewById<TextView>(R.id.textDirectoryExpandHint)
        private val expandedLayout = itemView.findViewById<View>(R.id.layoutDirectoryExpanded)
        private val eventsRecycler = itemView.findViewById<RecyclerView>(R.id.recyclerClassEvents)
        private val notes = itemView.findViewById<TextInputEditText>(R.id.editDirectoryNotes)
        private val saveNotes = itemView.findViewById<MaterialButton>(R.id.buttonSaveDirectoryNotes)

        fun bind(item: CourseProfile) {
            course.text = item.courseName
            section.text = itemView.context.getString(R.string.class_directory_section, item.section)
            meeting.text = itemView.context.getString(
                R.string.class_directory_meeting,
                item.meetingTimes
            )
            location.text = itemView.context.getString(R.string.event_location_label, item.location)
            val instructorLabel = item.profEmail.ifBlank {
                itemView.context.getString(R.string.class_directory_not_available)
            }
            instructor.text = itemView.context.getString(R.string.class_directory_instructor, instructorLabel)
            val taLabel = item.taEmail.ifBlank {
                itemView.context.getString(R.string.class_directory_not_available)
            }
            ta.text = itemView.context.getString(R.string.class_directory_ta, taLabel)
            grading.text = itemView.context.getString(R.string.class_directory_grading, item.gradingScale)
            notes.setText(item.notes)
            saveNotes.setOnClickListener {
                onSaveNotes(item, notes.text?.toString().orEmpty())
            }
            val classEvents = eventsByClass[item.courseName].orEmpty().sortedBy { it.eventDate }
            eventsRecycler.layoutManager = LinearLayoutManager(itemView.context)
            eventsRecycler.isNestedScrollingEnabled = true
            eventsRecycler.adapter = ClassEventsMiniAdapter(classEvents)
            val expanded = expandedProfileIds.contains(item.profileId)
            expandedLayout.visibility = if (expanded) View.VISIBLE else View.GONE
            expandHint.text = itemView.context.getString(
                if (expanded) R.string.class_collapse_hint else R.string.class_expand_hint
            )

            item.profEmail.takeIf { it.isNotBlank() }?.let { email ->
                instructor.setOnClickListener {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                    itemView.context.startActivity(intent)
                }
            } ?: run { instructor.setOnClickListener(null) }

            item.taEmail.takeIf { it.isNotBlank() }?.let { email ->
                ta.setOnClickListener {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                    itemView.context.startActivity(intent)
                }
            } ?: run { ta.setOnClickListener(null) }

            itemView.setOnClickListener {
                if (expanded) expandedProfileIds.remove(item.profileId) else expandedProfileIds.add(item.profileId)
                notifyItemChanged(bindingAdapterPosition)
            }
            delete.setOnClickListener {
                AlertDialog.Builder(itemView.context)
                    .setMessage(R.string.class_delete_confirm)
                    .setNegativeButton(R.string.registration_cancel, null)
                    .setPositiveButton(R.string.delete) { d, _ -> d.dismiss() }
                    .show()
            }
        }
    }
}

private class ClassEventsMiniAdapter(
    private val items: List<ExamEvent>
) : RecyclerView.Adapter<ClassEventsMiniAdapter.RowHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return RowHolder(view)
    }

    override fun onBindViewHolder(holder: RowHolder, position: Int) {
        holder.bind(items.getOrNull(position))
    }

    override fun getItemCount(): Int = if (items.isEmpty()) 1 else items.size

    inner class RowHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title = itemView.findViewById<TextView>(android.R.id.text1)
        private val subtitle = itemView.findViewById<TextView>(android.R.id.text2)

        fun bind(event: ExamEvent?) {
            if (event == null) {
                title.text = itemView.context.getString(R.string.class_event_list_empty)
                subtitle.text = ""
                return
            }
            val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(event.eventDate))
            title.text = event.examTitle
            subtitle.text = "$date  •  ${event.eventType}"
            title.textSize = 12f
            subtitle.textSize = 11f
            title.setTextColor(itemView.resources.getColor(R.color.notion_text_primary, null))
            subtitle.setTextColor(itemView.resources.getColor(R.color.notion_text_secondary, null))
        }
    }
}
