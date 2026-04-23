package com.example.semestra.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.semestra.R
import com.example.semestra.data.ExamEvent
import java.text.DateFormat
import java.util.Date

class ScheduleEventAdapter(
    private val onClick: (ExamEvent) -> Unit
) : RecyclerView.Adapter<ScheduleEventAdapter.Holder>() {

    private val items = mutableListOf<ExamEvent>()

    fun submitList(events: List<ExamEvent>) {
        items.clear()
        items.addAll(events)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule_event, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val classTag = view.findViewById<TextView>(R.id.textScheduleClassTag)
        private val title = view.findViewById<TextView>(R.id.textScheduleTitle)
        private val subtitle = view.findViewById<TextView>(R.id.textScheduleSubtitle)
        private val topic = view.findViewById<TextView>(R.id.textScheduleTopic)

        fun bind(event: ExamEvent) {
            classTag.text = event.className
            title.text = "${event.className} • ${event.startTime} - ${event.endTime}"
            val dateStr = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(event.eventDate))
            val syncStr = itemView.context.getString(
                if (event.synced) R.string.synced_label else R.string.not_synced_label
            )
            subtitle.text = itemView.context.getString(
                R.string.schedule_event_subtitle,
                event.eventType,
                dateStr,
                event.location.ifBlank { syncStr }
            )
            topic.text = itemView.context.getString(
                R.string.schedule_event_topic,
                event.examTitle
            )
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f * itemView.resources.displayMetrics.density
                val palette = listOf(
                    0xFFE8F0FE.toInt(),
                    0xFFE8F5E9.toInt(),
                    0xFFFFF3E0.toInt(),
                    0xFFF3E5F5.toInt(),
                    0xFFE0F7FA.toInt()
                )
                setColor(palette[kotlin.math.abs(event.className.hashCode()) % palette.size])
                setStroke(
                    (1f * itemView.resources.displayMetrics.density).toInt(),
                    ContextCompat.getColor(itemView.context, R.color.notion_stroke)
                )
            }
            classTag.background = bg
            itemView.setOnClickListener { onClick(event) }
        }
    }
}
