package com.example.dooralarm

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CustomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val timestamp_tv: TextView = itemView.findViewById(R.id.timestamp_tv)
    private val log_tv: TextView = itemView.findViewById(R.id.log_tv)
    // Other views in the item layout

    fun bindData(itemData: ItemData) {
        timestamp_tv.text = itemData.time
        log_tv.text = itemData.text
        // Bind data to other views
    }
}
