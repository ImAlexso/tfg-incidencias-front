package com.incidencias.ui.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.incidencias.R
import com.incidencias.databinding.ItemNotificationBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class NotificationAdapter(
    private val role: String,
    private val onClick: (NotificationUiModel) -> Unit,
    private val onDeleteClick: (NotificationUiModel) -> Unit
) : ListAdapter<NotificationUiModel, NotificationAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        val (title, body, badge) = buildPresentation(item)

        holder.binding.tvTitle.text = title
        holder.binding.tvBody.text = body
        holder.binding.tvType.text = badge
        holder.binding.tvReference.text = item.referenceCode ?: "Sin incidencia"
        holder.binding.tvDate.text = formatDate(item.createdAt)
        holder.binding.viewUnread.visibility = if (item.read) View.INVISIBLE else View.VISIBLE

        when (item.type) {
            NotificationType.INCIDENT_CREATED -> {
                holder.binding.ivIcon.setImageResource(android.R.drawable.ic_input_add)
                holder.binding.tvType.text = "Creada"
                holder.binding.tvType.setBackgroundResource(R.drawable.bg_badge_creada)
            }

            NotificationType.STATUS_CHANGED,
            NotificationType.INCIDENT_RESOLVED,
            NotificationType.INCIDENT_CLOSED -> {
                holder.binding.ivIcon.setImageResource(android.R.drawable.ic_popup_sync)
                holder.binding.tvType.text = "Estado"
                holder.binding.tvType.setBackgroundResource(R.drawable.bg_badge_estado)
            }

            NotificationType.MESSAGE_PUBLIC -> {
                holder.binding.ivIcon.setImageResource(android.R.drawable.ic_dialog_email)
                holder.binding.tvType.text = "Mensaje"
                holder.binding.tvType.setBackgroundResource(R.drawable.bg_badge_mensaje)
            }

            NotificationType.MESSAGE_INTERNAL -> {
                holder.binding.ivIcon.setImageResource(android.R.drawable.ic_dialog_email)
                holder.binding.tvType.text = "Interno"
                holder.binding.tvType.setBackgroundResource(R.drawable.bg_badge_interno)
            }

            NotificationType.ATTACHMENT_UPLOADED -> {
                holder.binding.ivIcon.setImageResource(android.R.drawable.ic_menu_upload)
                holder.binding.tvType.text = "Adjunto"
                holder.binding.tvType.setBackgroundResource(R.drawable.bg_badge_adjunto)
            }

            NotificationType.PRIORITY_CHANGED -> {
                holder.binding.ivIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                holder.binding.tvType.text = "Prioridad"
                holder.binding.tvType.setBackgroundResource(R.drawable.bg_badge_estado)
            }

            NotificationType.TEAM_CHANGED -> {
                holder.binding.ivIcon.setImageResource(android.R.drawable.ic_menu_manage)
                holder.binding.tvType.text = "Equipo"
                holder.binding.tvType.setBackgroundResource(R.drawable.bg_badge_estado)
            }

            NotificationType.TECHNICIAN_ASSIGNED,
            NotificationType.ASSIGNEE_REMOVED -> {
                holder.binding.ivIcon.setImageResource(android.R.drawable.ic_menu_manage)
                holder.binding.tvType.text = "Asignada"
                holder.binding.tvType.setBackgroundResource(R.drawable.bg_badge_asignada)
            }

            NotificationType.UNKNOWN -> {
                holder.binding.ivIcon.setImageResource(android.R.drawable.ic_dialog_info)
                holder.binding.tvType.text = "General"
                holder.binding.tvType.setBackgroundResource(R.drawable.bg_badge_adjunto)
            }
        }

        holder.binding.root.setOnClickListener {
            onClick(item)
        }

        holder.binding.btnDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    private fun formatDate(raw: String): String {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale("es"))

        return try {
            val parsed = LocalDateTime.parse(raw)
            parsed.format(formatter)
        } catch (_: Exception) {
            raw
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<NotificationUiModel>() {
            override fun areItemsTheSame(
                oldItem: NotificationUiModel,
                newItem: NotificationUiModel
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: NotificationUiModel,
                newItem: NotificationUiModel
            ): Boolean = oldItem == newItem
        }
    }
    private fun buildPresentation(item: NotificationUiModel): Triple<String, String, String> {
        return when (role) {
            "USER" -> buildUserPresentation(item)
            "TECHNICIAN" -> buildTechnicianPresentation(item)
            "MANAGER" -> buildManagerPresentation(item)
            else -> Triple(item.actorName ?: "", item.body, "General")
        }
    }
    private fun buildUserPresentation(item: NotificationUiModel): Triple<String, String, String> {
        return when (item.type) {

            NotificationType.INCIDENT_CREATED ->
                Triple("Incidencia creada", "Tu incidencia ha sido registrada correctamente", "Creada")

            NotificationType.STATUS_CHANGED ->
                Triple("Estado actualizado", "El estado de tu incidencia ha cambiado", "Estado")

            NotificationType.INCIDENT_RESOLVED ->
                Triple("Incidencia resuelta", "Tu incidencia ha sido resuelta", "Estado")

            NotificationType.INCIDENT_CLOSED ->
                Triple("Incidencia cerrada", "Tu incidencia ha sido cerrada", "Estado")

            NotificationType.TECHNICIAN_ASSIGNED ->
                Triple("Soporte", "Tu incidencia ya tiene un técnico asignado", "Estado")

            NotificationType.ASSIGNEE_REMOVED ->
                Triple("Soporte", "Tu incidencia ha quedado sin técnico asignado", "Estado")

            NotificationType.MESSAGE_PUBLIC ->
                Triple(item.actorName ?: "Mensaje", "Ha añadido un mensaje en la incidencia", "Mensaje")

            NotificationType.ATTACHMENT_UPLOADED ->
                Triple("Adjunto añadido", "Se ha añadido un archivo a tu incidencia", "Adjunto")

            else -> Triple(item.actorName ?: "", item.body, "General")
        }
    }
    private fun buildTechnicianPresentation(item: NotificationUiModel): Triple<String, String, String> {
        return when (item.type) {

            NotificationType.INCIDENT_CREATED ->
                Triple("Nueva incidencia", "Se ha creado una nueva incidencia", "Creada")

            NotificationType.TECHNICIAN_ASSIGNED ->
                Triple("Asignación", "Se te ha asignado una incidencia", "Asignada")

            NotificationType.ASSIGNEE_REMOVED ->
                Triple("Asignación", "Se te ha retirado una incidencia", "Asignada")

            NotificationType.STATUS_CHANGED ->
                Triple("Estado actualizado", "La incidencia ha cambiado de estado", "Estado")

            NotificationType.INCIDENT_RESOLVED ->
                Triple("Incidencia resuelta", "La incidencia ha sido resuelta", "Estado")

            NotificationType.MESSAGE_PUBLIC,
            NotificationType.MESSAGE_INTERNAL ->
                Triple(item.actorName ?: "Mensaje", "Ha añadido un mensaje en la incidencia", "Mensaje")

            NotificationType.ATTACHMENT_UPLOADED ->
                Triple("Adjunto añadido", "Se ha añadido un archivo a la incidencia", "Adjunto")

            else -> Triple(item.actorName ?: "", item.body, "General")
        }
    }
    private fun buildManagerPresentation(item: NotificationUiModel): Triple<String, String, String> {
        return when (item.type) {

            NotificationType.INCIDENT_CREATED ->
                Triple("Incidencia creada", "Se ha creado una nueva incidencia", "Creada")

            NotificationType.TECHNICIAN_ASSIGNED ->
                Triple("Asignación", "Se ha asignado un técnico a la incidencia", "Estado")

            NotificationType.ASSIGNEE_REMOVED ->
                Triple("Asignación", "Se ha retirado el técnico asignado", "Estado")

            NotificationType.STATUS_CHANGED ->
                Triple("Estado actualizado", "La incidencia ha cambiado de estado", "Estado")

            NotificationType.INCIDENT_RESOLVED ->
                Triple("Incidencia resuelta", "La incidencia ha sido resuelta", "Estado")

            NotificationType.MESSAGE_PUBLIC,
            NotificationType.MESSAGE_INTERNAL ->
                Triple(item.actorName ?: "Mensaje", "Ha añadido un mensaje en la incidencia", "Mensaje")

            else -> Triple(item.actorName ?: "", item.body, "General")
        }
    }
}