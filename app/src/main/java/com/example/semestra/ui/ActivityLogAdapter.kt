package com.example.semestra.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.semestra.R
import com.example.semestra.data.ActivityLog
import java.text.DateFormat
import java.util.Date

class ActivityLogAdapter : RecyclerView.Adapter<ActivityLogAdapter.Holder>() {
    private val items = mutableListOf<ActivityLog>()
    private var onClick: ((ActivityLog) -> Unit)? = null

    fun setOnClickListener(listener: (ActivityLog) -> Unit) {
        onClick = listener
    }

    fun submitList(newItems: List<ActivityLog>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getItemAt(position: Int): ActivityLog? = items.getOrNull(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity_log, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon = itemView.findViewById<ImageView>(R.id.imageLogType)
        private val title = itemView.findViewById<TextView>(R.id.textLogTitle)
        private val typeTag = itemView.findViewById<TextView>(R.id.textLogTypeTag)
        private val details = itemView.findViewById<TextView>(R.id.textLogDetails)

        fun bind(item: ActivityLog) {
            val type = ActivityType.from(item.title)
            title.text = item.title
            typeTag.text = itemView.context.getString(type.labelRes)
            val time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(Date(item.createdAt))
            details.text = itemView.context.getString(R.string.activity_log_row, item.details, time)
            icon.setImageResource(type.iconRes)
            val tint = ContextCompat.getColor(itemView.context, type.tintRes)
            icon.setColorFilter(tint)
            typeTag.setTextColor(tint)
            itemView.setOnClickListener { onClick?.invoke(item) }
        }
    }

    private enum class ActivityType(val labelRes: Int, val iconRes: Int, val tintRes: Int) {
        PARSED(
            R.string.activity_type_parsed,
            android.R.drawable.ic_menu_upload,
            R.color.notion_text_primary
        ),
        SYNCED(
            R.string.activity_type_synced,
            android.R.drawable.ic_popup_sync,
            R.color.notion_text_secondary
        ),
        REMOVED(
            R.string.activity_type_removed,
            android.R.drawable.ic_menu_delete,
            R.color.notion_error
        ),
        OTHER(
            R.string.activity_type_other,
            android.R.drawable.ic_menu_info_details,
            R.color.notion_text_secondary
        );

        companion object {
            fun from(title: String): ActivityType {
                return when {
                    title.contains("parsed", ignoreCase = true) -> PARSED
                    title.contains("sync", ignoreCase = true) -> SYNCED
                    title.contains("removed", ignoreCase = true) || title.contains("delete", ignoreCase = true) -> REMOVED
                    else -> OTHER
                }
            }
        }
    }
}
