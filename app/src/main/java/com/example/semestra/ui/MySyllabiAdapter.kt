package com.example.semestra.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.semestra.R
import com.example.semestra.data.Syllabus
import java.text.DateFormat
import java.util.Date

class MySyllabiAdapter(
    private val onClick: (Syllabus) -> Unit
) : RecyclerView.Adapter<MySyllabiAdapter.Holder>() {
    private val items = mutableListOf<Syllabus>()

    fun submitList(newItems: List<Syllabus>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_syllabus, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title = itemView.findViewById<TextView>(R.id.textSyllabusTitle)
        private val meta = itemView.findViewById<TextView>(R.id.textSyllabusMeta)

        fun bind(item: Syllabus) {
            title.text = item.courseSection ?: itemView.context.getString(R.string.my_syllabus_unknown_course)
            val uploaded = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(item.uploadDate))
            val instructor = item.instructorName ?: itemView.context.getString(R.string.my_syllabus_unknown_instructor)
            meta.text = itemView.context.getString(R.string.my_syllabus_row_meta, instructor, uploaded)
            itemView.setOnClickListener { onClick(item) }
        }
    }
}
