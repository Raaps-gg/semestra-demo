package com.example.semestra.ui

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.semestra.R
import com.example.semestra.data.Syllabus

class ClassDirectoryAdapter : RecyclerView.Adapter<ClassDirectoryAdapter.Holder>() {
    private val items = mutableListOf<Syllabus>()

    fun submitList(newItems: List<Syllabus>) {
        items.clear()
        items.addAll(newItems)
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
        private val meeting = itemView.findViewById<TextView>(R.id.textDirectoryMeeting)
        private val instructor = itemView.findViewById<TextView>(R.id.textDirectoryInstructor)
        private val ta = itemView.findViewById<TextView>(R.id.textDirectoryTA)
        private val grading = itemView.findViewById<TextView>(R.id.textDirectoryGrading)

        fun bind(item: Syllabus) {
            course.text = item.courseSection ?: itemView.context.getString(R.string.my_syllabus_unknown_course)
            meeting.text = itemView.context.getString(
                R.string.class_directory_meeting,
                item.meetingInfo ?: itemView.context.getString(R.string.class_directory_not_available)
            )
            val instructorLabel = listOfNotNull(item.instructorName, item.instructorEmail).joinToString(" • ")
                .ifBlank { itemView.context.getString(R.string.class_directory_not_available) }
            instructor.text = itemView.context.getString(R.string.class_directory_instructor, instructorLabel)
            val taLabel = listOfNotNull(item.taName, item.taEmail).joinToString(" • ")
                .ifBlank { itemView.context.getString(R.string.class_directory_not_available) }
            ta.text = itemView.context.getString(R.string.class_directory_ta, taLabel)
            grading.text = itemView.context.getString(
                R.string.class_directory_grading,
                item.gradingSummary ?: itemView.context.getString(R.string.class_directory_not_available)
            )

            item.instructorEmail?.takeIf { it.isNotBlank() }?.let { email ->
                instructor.setOnClickListener {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                    itemView.context.startActivity(intent)
                }
            } ?: run { instructor.setOnClickListener(null) }

            item.taEmail?.takeIf { it.isNotBlank() }?.let { email ->
                ta.setOnClickListener {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                    itemView.context.startActivity(intent)
                }
            } ?: run { ta.setOnClickListener(null) }
        }
    }
}
