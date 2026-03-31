package com.incidencias.ui.common.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.incidencias.R
import com.incidencias.data.remote.dto.incident.IncidentListItemResponse
import com.incidencias.databinding.ItemIncidentBinding
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class IncidentAdapter(
    private val showAssignedTechnician: Boolean = false,
    private val showAssignToMeAction: Boolean = false,
    private val onItemClick: (IncidentListItemResponse) -> Unit,
    private val onAssignToMeClick: ((IncidentListItemResponse) -> Unit)? = null
) : ListAdapter<IncidentListItemResponse, IncidentAdapter.ViewHolder>(DiffCallback) {

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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        holder.binding.tvReference.text = item.referenceCode
        holder.binding.tvTitle.text = item.title
        holder.binding.tvStatus.text = item.statusName
        holder.binding.tvPriority.text = item.priorityName ?: "-"
        val createdAtText = formatCreatedAt(item.createdAt)
        holder.binding.tvCreatedAt.text = createdAtText
        holder.binding.tvCreatedAt.visibility = if (createdAtText.isBlank()) View.GONE else View.VISIBLE

        val subtitleParts = buildList {
            add(
                if (!item.currentTeamName.isNullOrBlank()) {
                    "Equipo: ${item.currentTeamName}"
                } else {
                    "Equipo sin asignar"
                }
            )

            if (showAssignedTechnician) {
                add(
                    if (item.isAssignedToCurrentUser) {
                        "Asignada a ti"
                    } else if (!item.assignedTechnicianEmail.isNullOrBlank()) {
                        "Técnico: ${item.assignedTechnicianEmail}"
                    } else {
                        "Sin técnico asignado"
                    }
                )
            }
        }

        holder.binding.tvSubtitle.text = subtitleParts.joinToString(separator = " · ")

        when (item.statusName.uppercase()) {
            "OPEN" -> holder.binding.tvStatus.setBackgroundResource(R.drawable.bg_status_open)
            "IN_PROGRESS" -> holder.binding.tvStatus.setBackgroundResource(R.drawable.bg_neutral_chip)
            "RESOLVED" -> holder.binding.tvStatus.setBackgroundResource(R.drawable.bg_status_resolved)
            "CLOSED" -> holder.binding.tvStatus.setBackgroundResource(R.drawable.bg_status_closed)
            else -> holder.binding.tvStatus.setBackgroundResource(R.drawable.bg_status_open)
        }

        when (item.priorityName?.uppercase()) {
            "CRITICAL" -> holder.binding.tvPriority.setBackgroundResource(R.drawable.bg_priority_critical)
            "HIGH" -> holder.binding.tvPriority.setBackgroundResource(R.drawable.bg_priority_high)
            "MEDIUM" -> holder.binding.tvPriority.setBackgroundResource(R.drawable.bg_priority_medium)
            "LOW" -> holder.binding.tvPriority.setBackgroundResource(R.drawable.bg_priority_low)
            else -> holder.binding.tvPriority.setBackgroundResource(R.drawable.bg_priority_low)
        }

        val showAssignButton = showAssignToMeAction && item.canAssignToMe && item.assignedTechnicianId == null
        holder.binding.btnAssignToMe.visibility = if (showAssignButton) View.VISIBLE else View.GONE
        holder.binding.btnAssignToMe.setOnClickListener {
            onAssignToMeClick?.invoke(item)
        }

        holder.binding.root.setOnClickListener { onItemClick(item) }
    }

    private fun formatCreatedAt(value: String?): String {
        return try {
            val parsed = if (value.isNullOrBlank()) null else OffsetDateTime.parse(value)
            if (parsed == null) {
                ""
            } else {
                "Creada: ${parsed.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}"
            }
        } catch (_: Exception) {
            ""
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<IncidentListItemResponse>() {
            override fun areItemsTheSame(
                oldItem: IncidentListItemResponse,
                newItem: IncidentListItemResponse
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: IncidentListItemResponse,
                newItem: IncidentListItemResponse
            ): Boolean = oldItem == newItem
        }
    }
}
