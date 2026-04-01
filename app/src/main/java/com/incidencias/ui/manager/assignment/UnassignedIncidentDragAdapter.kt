package com.incidencias.ui.manager.assignment

import android.content.ClipData
import android.content.ClipDescription
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.incidencias.data.remote.dto.incident.IncidentListItemResponse
import com.incidencias.databinding.ItemDragIncidentBinding

class UnassignedIncidentDragAdapter(
    private var items: List<IncidentListItemResponse>,
    private val onClick: (IncidentListItemResponse) -> Unit
) : RecyclerView.Adapter<UnassignedIncidentDragAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemDragIncidentBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun submitList(newItems: List<IncidentListItemResponse>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDragIncidentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.binding.tvReference.text = item.referenceCode
        holder.binding.tvTitle.text = item.title
        holder.binding.tvPriority.text = item.priorityName ?: "-"

        holder.binding.root.setOnClickListener {
            onClick(item)
        }

        holder.binding.root.setOnLongClickListener { view ->
            val clipData = ClipData(
                "incident",
                arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                ClipData.Item(item.id.toString())
            )

            val shadow = android.view.View.DragShadowBuilder(view)
            view.startDragAndDrop(clipData, shadow, item, 0)
            true
        }
    }
}