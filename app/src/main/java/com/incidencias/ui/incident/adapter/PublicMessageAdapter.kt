package com.incidencias.ui.incident.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.incidencias.data.remote.dto.message.IncidentMessageResponse
import com.incidencias.databinding.ItemPublicMessageBinding

class PublicMessageAdapter(
    private var items: List<IncidentMessageResponse>
) : RecyclerView.Adapter<PublicMessageAdapter.ViewHolder>() {

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
        holder.binding.tvAuthor.text = item.authorEmail
        holder.binding.tvCreatedAt.text = item.createdAt
        holder.binding.tvMessage.text = item.message
    }

    fun updateData(newItems: List<IncidentMessageResponse>) {
        items = newItems
        notifyDataSetChanged()
    }
}