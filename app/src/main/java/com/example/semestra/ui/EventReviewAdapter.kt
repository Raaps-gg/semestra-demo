package com.example.semestra.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.semestra.R
import com.example.semestra.data.ExamEvent
import java.text.DateFormat
import java.util.Date

class EventReviewAdapter(
    private val onEdit: (ExamEvent) -> Unit,
    private val onDelete: (ExamEvent) -> Unit
) : RecyclerView.Adapter<EventReviewAdapter.Holder>() {

    private val items = mutableListOf<ExamEvent>()

    fun submitList(events: List<ExamEvent>) {
        items.clear()
        items.addAll(events)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_review, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val warning: ImageView = itemView.findViewById(R.id.imageWarning)
        private val title: TextView = itemView.findViewById(R.id.textEventTitle)
        private val meta: TextView = itemView.findViewById(R.id.textEventMeta)

        fun bind(event: ExamEvent) {
            title.text = event.examTitle
            val dateStr = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(event.eventDate))
            meta.text = itemView.context.getString(R.string.review_event_meta, event.className, dateStr)
            warning.visibility = if (event.needsReview) View.VISIBLE else View.GONE
            itemView.findViewById<View>(R.id.buttonEditEvent).setOnClickListener { onEdit(event) }
            itemView.findViewById<View>(R.id.buttonDeleteEvent).setOnClickListener { onDelete(event) }
        }
    }
}
