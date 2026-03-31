package com.incidencias.ui.incident.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.incidencias.data.remote.dto.incident.IncidentEventResponse
import com.incidencias.databinding.ItemPublicMessageBinding

class HistoryAdapter(
    private var items: List<IncidentEventResponse>
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemPublicMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPublicMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvAuthor.text = item.eventType
        holder.binding.tvCreatedAt.text = item.createdAt
        holder.binding.tvMessage.text = item.eventDescription
    }

    fun updateData(newItems: List<IncidentEventResponse>) {
        items = newItems
        notifyDataSetChanged()
    }
}