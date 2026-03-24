package com.incidencias.ui.common.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.incidencias.data.remote.dto.incident.IncidentListItemResponse
import com.incidencias.databinding.ItemIncidentBinding

class IncidentAdapter(
    private val items: List<IncidentListItemResponse>,
    private val onItemClick: (IncidentListItemResponse) -> Unit
) : RecyclerView.Adapter<IncidentAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemIncidentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIncidentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.binding.tvReference.text = item.referenceCode
        holder.binding.tvTitle.text = item.title
        holder.binding.tvStatus.text = item.statusName
        holder.binding.tvPriority.text = item.priorityName ?: "-"
        holder.binding.root.setOnClickListener { onItemClick(item) }
    }
}