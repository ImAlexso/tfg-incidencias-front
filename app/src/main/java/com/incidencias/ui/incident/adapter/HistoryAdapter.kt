package com.incidencias.ui.incident.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.incidencias.data.remote.dto.incident.IncidentEventResponse
import com.incidencias.databinding.ItemPublicMessageBinding
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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

        holder.binding.tvAuthor.text = mapEventTitle(item.eventType)
        holder.binding.tvCreatedAt.text = formatDate(item.createdAt)
        holder.binding.tvMessage.text = mapEventDescription(item)
    }

    fun updateData(newItems: List<IncidentEventResponse>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun mapEventTitle(type: String): String {
        return when (type) {
            "INCIDENT_CREATED" -> "Incidencia creada"
            "STATUS_CHANGED" -> "Estado actualizado"
            "PRIORITY_CHANGED" -> "Prioridad actualizada"
            "TEAM_CHANGED" -> "Equipo actualizado"
            "TEAM_ASSIGNED" -> "Equipo asignado"
            "TECHNICIAN_ASSIGNED" -> "Técnico asignado"
            "ASSIGNEE_REMOVED" -> "Técnico eliminado"
            "MESSAGE_PUBLIC" -> "Mensaje añadido"
            "MESSAGE_INTERNAL" -> "Mensaje interno"
            "ATTACHMENT_UPLOADED" -> "Adjunto subido"
            "INCIDENT_RESOLVED" -> "Incidencia resuelta"
            "INCIDENT_CLOSED" -> "Incidencia cerrada"
            "AI_CLASSIFICATION_ATTEMPTED" -> "Clasificación automática iniciada"
            "AI_CLASSIFICATION_APPLIED" -> "Clasificación automática aplicada"
            "AI_CLASSIFICATION_FAILED" -> "Clasificación automática fallida"
            else -> type
        }
    }

    private fun mapEventDescription(item: IncidentEventResponse): String {
        return when (item.eventType) {
            "INCIDENT_CREATED" -> "Se ha creado la incidencia"
            "STATUS_CHANGED" -> "El estado ha sido actualizado"
            "PRIORITY_CHANGED" -> "La prioridad ha sido modificada"
            "TEAM_CHANGED" -> "El equipo ha sido actualizado"
            "TEAM_ASSIGNED" -> "La incidencia ha sido asignada a un equipo"
            "TECHNICIAN_ASSIGNED" -> "Se ha asignado un técnico"
            "ASSIGNEE_REMOVED" -> "Se ha eliminado el técnico asignado"
            "MESSAGE_PUBLIC" -> "Se ha añadido un mensaje"
            "MESSAGE_INTERNAL" -> "Se ha añadido un mensaje interno"
            "ATTACHMENT_UPLOADED" -> "Se ha añadido un adjunto"
            "INCIDENT_RESOLVED" -> "La incidencia ha sido resuelta"
            "INCIDENT_CLOSED" -> "La incidencia ha sido cerrada"
            "AI_CLASSIFICATION_ATTEMPTED" -> "Se ha intentado clasificar automáticamente la incidencia"
            "AI_CLASSIFICATION_APPLIED" -> "La clasificación automática se ha aplicado correctamente"
            "AI_CLASSIFICATION_FAILED" -> "La clasificación automática no se ha podido aplicar"
            else -> item.eventDescription
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