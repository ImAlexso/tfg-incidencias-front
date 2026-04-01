package com.incidencias.ui.manager.assignment

import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.incidencias.data.remote.dto.catalog.TeamTechnicianResponse
import com.incidencias.databinding.ItemDropTechnicianBinding
import android.content.ClipDescription

class TechnicianDropAdapter(
    private var items: List<TeamTechnicianResponse>,
    private val onIncidentDropped: (incidentId: Long, technician: TeamTechnicianResponse) -> Unit
) : RecyclerView.Adapter<TechnicianDropAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemDropTechnicianBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun submitList(newItems: List<TeamTechnicianResponse>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDropTechnicianBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val technician = items[position]

        holder.binding.tvName.text = technician.fullName
        holder.binding.tvEmail.text = technician.email

        holder.binding.root.setOnDragListener { view, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    event.clipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true
                }

                DragEvent.ACTION_DRAG_ENTERED -> {
                    view.alpha = 0.7f
                    true
                }

                DragEvent.ACTION_DRAG_EXITED -> {
                    view.alpha = 1f
                    true
                }

                DragEvent.ACTION_DROP -> {
                    view.alpha = 1f
                    val draggedIncident = event.localState
                    val incidentId = when (draggedIncident) {
                        is com.incidencias.data.remote.dto.incident.IncidentListItemResponse -> draggedIncident.id
                        else -> event.clipData.getItemAt(0).text.toString().toLongOrNull()
                    }

                    if (incidentId != null) {
                        onIncidentDropped(incidentId, technician)
                    }
                    true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    view.alpha = 1f
                    true
                }

                else -> true
            }
        }
    }
}