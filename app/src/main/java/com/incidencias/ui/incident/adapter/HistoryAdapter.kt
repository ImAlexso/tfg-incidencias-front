package com.incidencias.ui.incident.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.incidencias.R
import com.incidencias.data.remote.dto.incident.IncidentEventResponse
import com.incidencias.databinding.ItemHistoryEventBinding
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class HistoryAdapter(
    private var items: List<IncidentEventResponse>
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemHistoryEventBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.binding.root.context

        holder.binding.tvEventTitle.text = mapEventTitle(item.eventType)
        holder.binding.tvEventDate.text = formatDate(item.createdAt)
        holder.binding.tvEventDescription.text = mapEventDescription(item)

        when (item.eventType.uppercase()) {
            "INCIDENT_CREATED" -> {
                holder.binding.ivEventIcon.setImageResource(R.drawable.ic_history_plus_small)
                holder.binding.ivEventIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.primary)
                )
            }

            "STATUS_CHANGED",
            "INCIDENT_RESOLVED",
            "INCIDENT_CLOSED" -> {
                holder.binding.ivEventIcon.setImageResource(R.drawable.ic_history_status_small)
                holder.binding.ivEventIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.success)
                )
            }

            "PRIORITY_CHANGED" -> {
                holder.binding.ivEventIcon.setImageResource(R.drawable.ic_history_flag_small)
                holder.binding.ivEventIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.warning)
                )
            }

            "TEAM_CHANGED",
            "TEAM_ASSIGNED",
            "TECHNICIAN_ASSIGNED",
            "ASSIGNEE_REMOVED" -> {
                holder.binding.ivEventIcon.setImageResource(R.drawable.ic_role_manager_small)
                holder.binding.ivEventIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.badge_internal)
                )
            }

            "MESSAGE_PUBLIC",
            "MESSAGE_INTERNAL",
            "MESSAGE_ADDED",
            "INTERNAL_MESSAGE_ADDED" -> {
                holder.binding.ivEventIcon.setImageResource(R.drawable.ic_history_message_small)
                holder.binding.ivEventIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.primary)
                )
            }

            "ATTACHMENT_UPLOADED",
            "ATTACHMENT_ADDED" -> {
                holder.binding.ivEventIcon.setImageResource(R.drawable.ic_notification_attachment)
                holder.binding.ivEventIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.badge_attachment)
                )
            }

            "AI_CLASSIFICATION_ATTEMPTED",
            "AI_CLASSIFICATION_APPLIED",
            "AI_CLASSIFICATION_FAILED" -> {
                holder.binding.ivEventIcon.setImageResource(R.drawable.ic_history_ai_small)
                holder.binding.ivEventIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.badge_internal)
                )
            }

            else -> {
                holder.binding.ivEventIcon.setImageResource(R.drawable.ic_history_activity_small)
                holder.binding.ivEventIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.text_muted)
                )
            }
        }
    }

    fun updateData(newItems: List<IncidentEventResponse>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun mapEventTitle(type: String): String {
        return when (type.uppercase()) {
            "INCIDENT_CREATED" -> "Incidencia creada"
            "STATUS_CHANGED" -> "Estado actualizado"
            "PRIORITY_CHANGED" -> "Prioridad actualizada"
            "TEAM_CHANGED" -> "Equipo actualizado"
            "TEAM_ASSIGNED" -> "Equipo asignado"
            "TECHNICIAN_ASSIGNED" -> "Técnico asignado"
            "ASSIGNEE_REMOVED" -> "Técnico eliminado"
            "MESSAGE_PUBLIC", "MESSAGE_ADDED" -> "Mensaje añadido"
            "MESSAGE_INTERNAL", "INTERNAL_MESSAGE_ADDED" -> "Mensaje interno"
            "ATTACHMENT_UPLOADED", "ATTACHMENT_ADDED" -> "Adjunto subido"
            "INCIDENT_RESOLVED" -> "Incidencia resuelta"
            "INCIDENT_CLOSED" -> "Incidencia cerrada"
            "AI_CLASSIFICATION_ATTEMPTED" -> "Clasificación automática iniciada"
            "AI_CLASSIFICATION_APPLIED" -> "Clasificación automática aplicada"
            "AI_CLASSIFICATION_FAILED" -> "Clasificación automática fallida"
            else -> type.replace("_", " ")
        }
    }

    private fun mapEventDescription(item: IncidentEventResponse): String {
        return when (item.eventType.uppercase()) {
            "INCIDENT_CREATED" -> "Se ha creado la incidencia"
            "STATUS_CHANGED" -> "El estado ha sido actualizado"
            "PRIORITY_CHANGED" -> "La prioridad ha sido modificada"
            "TEAM_CHANGED" -> "El equipo ha sido actualizado"
            "TEAM_ASSIGNED" -> "La incidencia ha sido asignada a un equipo"
            "TECHNICIAN_ASSIGNED" -> "Se ha asignado un técnico"
            "ASSIGNEE_REMOVED" -> "Se ha eliminado el técnico asignado"
            "MESSAGE_PUBLIC", "MESSAGE_ADDED" -> "Se ha añadido un mensaje"
            "MESSAGE_INTERNAL", "INTERNAL_MESSAGE_ADDED" -> "Se ha añadido un mensaje interno"
            "ATTACHMENT_UPLOADED", "ATTACHMENT_ADDED" -> "Se ha añadido un adjunto"
            "INCIDENT_RESOLVED" -> "La incidencia ha sido resuelta"
            "INCIDENT_CLOSED" -> "La incidencia ha sido cerrada"
            "AI_CLASSIFICATION_ATTEMPTED" -> "Se ha intentado clasificar automáticamente la incidencia"
            "AI_CLASSIFICATION_APPLIED" -> "La clasificación automática se ha aplicado correctamente"
            "AI_CLASSIFICATION_FAILED" -> "La clasificación automática no se ha podido aplicar"
            else -> item.eventDescription.ifBlank { "Actividad registrada en la incidencia" }
        }
    }

    private fun formatDate(dateString: String): String {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale("es"))

        return try {
            OffsetDateTime.parse(dateString).format(formatter)
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(dateString).format(formatter)
            } catch (_: Exception) {
                dateString
            }
        }
    }
}