package com.incidencias.ui.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
        val presentation = buildPresentation(item)
        val style = buildStyle(item.type)

        with(holder.binding) {
            tvTitle.text = presentation.title
            tvBody.text = presentation.body
            tvType.text = style.badgeText
            tvReference.text = item.referenceCode ?: "Sin incidencia"
            tvDate.text = formatDate(item.createdAt)

            viewUnread.visibility = if (item.read) View.INVISIBLE else View.VISIBLE

            ivIcon.setImageResource(style.iconRes)
            ivIcon.imageTintList = ContextCompat.getColorStateList(root.context, style.iconTintRes)

            tvType.setBackgroundResource(style.badgeBackgroundRes)
            tvType.setTextColor(ContextCompat.getColor(root.context, style.badgeTextColorRes))

            root.alpha = if (item.read) 0.92f else 1f

            root.setOnClickListener { onClick(item) }
            btnDelete.setOnClickListener { onDeleteClick(item) }
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

    private data class NotificationPresentation(
        val title: String,
        val body: String
    )

    private data class NotificationStyle(
        val iconRes: Int,
        val iconTintRes: Int,
        val badgeText: String,
        val badgeBackgroundRes: Int,
        val badgeTextColorRes: Int
    )

    private fun buildPresentation(item: NotificationUiModel): NotificationPresentation {
        val (title, body, _) = when (role) {
            "USER" -> buildUserPresentation(item)
            "TECHNICIAN" -> buildTechnicianPresentation(item)
            "MANAGER" -> buildManagerPresentation(item)
            else -> Triple(item.actorName ?: "General", item.body, "General")
        }

        return NotificationPresentation(
            title = title,
            body = body
        )
    }

    private fun buildStyle(type: NotificationType): NotificationStyle {
        return when (type) {
            NotificationType.INCIDENT_CREATED -> NotificationStyle(
                iconRes = R.drawable.ic_notification_created,
                iconTintRes = R.color.primary,
                badgeText = "Creada",
                badgeBackgroundRes = R.drawable.bg_badge_created_soft,
                badgeTextColorRes = R.color.primary
            )

            NotificationType.STATUS_CHANGED,
            NotificationType.INCIDENT_RESOLVED,
            NotificationType.INCIDENT_CLOSED,
            NotificationType.PRIORITY_CHANGED,
            NotificationType.TEAM_CHANGED -> NotificationStyle(
                iconRes = R.drawable.ic_notification_status,
                iconTintRes = R.color.info,
                badgeText = "Estado",
                badgeBackgroundRes = R.drawable.bg_badge_status_soft,
                badgeTextColorRes = R.color.info
            )

            NotificationType.MESSAGE_PUBLIC -> NotificationStyle(
                iconRes = R.drawable.ic_notification_message,
                iconTintRes = R.color.success,
                badgeText = "Mensaje",
                badgeBackgroundRes = R.drawable.bg_badge_message_soft,
                badgeTextColorRes = R.color.success
            )

            NotificationType.MESSAGE_INTERNAL -> NotificationStyle(
                iconRes = R.drawable.ic_notification_message,
                iconTintRes = R.color.warning,
                badgeText = "Interno",
                badgeBackgroundRes = R.drawable.bg_badge_internal_soft,
                badgeTextColorRes = R.color.warning
            )

            NotificationType.ATTACHMENT_UPLOADED -> NotificationStyle(
                iconRes = R.drawable.ic_notification_attachment,
                iconTintRes = R.color.secondaryText,
                badgeText = "Adjunto",
                badgeBackgroundRes = R.drawable.bg_badge_attachment_soft,
                badgeTextColorRes = R.color.secondaryText
            )

            NotificationType.TECHNICIAN_ASSIGNED,
            NotificationType.ASSIGNEE_REMOVED -> NotificationStyle(
                iconRes = R.drawable.ic_notification_assignment,
                iconTintRes = R.color.warning,
                badgeText = "Asignación",
                badgeBackgroundRes = R.drawable.bg_badge_assignment_soft,
                badgeTextColorRes = R.color.warning
            )

            NotificationType.UNKNOWN -> NotificationStyle(
                iconRes = R.drawable.ic_notification_info,
                iconTintRes = R.color.secondaryText,
                badgeText = "General",
                badgeBackgroundRes = R.drawable.bg_neutral_chip,
                badgeTextColorRes = R.color.onSurface
            )
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
                Triple("Soporte asignado", "Tu incidencia ya tiene un técnico asignado", "Asignación")

            NotificationType.ASSIGNEE_REMOVED ->
                Triple("Asignación retirada", "Tu incidencia ha quedado sin técnico asignado", "Asignación")

            NotificationType.MESSAGE_PUBLIC ->
                Triple(item.actorName ?: "Nuevo mensaje", "Han añadido un mensaje en la incidencia", "Mensaje")

            NotificationType.ATTACHMENT_UPLOADED ->
                Triple("Adjunto añadido", "Se ha añadido un archivo a tu incidencia", "Adjunto")

            else ->
                Triple(item.actorName ?: "General", item.body, "General")
        }
    }

    private fun buildTechnicianPresentation(item: NotificationUiModel): Triple<String, String, String> {
        return when (item.type) {
            NotificationType.INCIDENT_CREATED ->
                Triple("Nueva incidencia", "Se ha creado una nueva incidencia", "Creada")

            NotificationType.TECHNICIAN_ASSIGNED ->
                Triple("Nueva asignación", "Se te ha asignado una incidencia", "Asignación")

            NotificationType.ASSIGNEE_REMOVED ->
                Triple("Asignación retirada", "Se te ha retirado una incidencia", "Asignación")

            NotificationType.STATUS_CHANGED ->
                Triple("Estado actualizado", "La incidencia ha cambiado de estado", "Estado")

            NotificationType.INCIDENT_RESOLVED ->
                Triple("Incidencia resuelta", "La incidencia ha sido resuelta", "Estado")

            NotificationType.MESSAGE_PUBLIC,
            NotificationType.MESSAGE_INTERNAL ->
                Triple(item.actorName ?: "Nuevo mensaje", "Han añadido un mensaje en la incidencia", "Mensaje")

            NotificationType.ATTACHMENT_UPLOADED ->
                Triple("Adjunto añadido", "Se ha añadido un archivo a la incidencia", "Adjunto")

            else ->
                Triple(item.actorName ?: "General", item.body, "General")
        }
    }

    private fun buildManagerPresentation(item: NotificationUiModel): Triple<String, String, String> {
        return when (item.type) {
            NotificationType.INCIDENT_CREATED ->
                Triple("Incidencia creada", "Se ha creado una nueva incidencia", "Creada")

            NotificationType.TECHNICIAN_ASSIGNED ->
                Triple("Técnico asignado", "Se ha asignado un técnico a la incidencia", "Asignación")

            NotificationType.ASSIGNEE_REMOVED ->
                Triple("Técnico retirado", "Se ha retirado el técnico asignado", "Asignación")

            NotificationType.STATUS_CHANGED ->
                Triple("Estado actualizado", "La incidencia ha cambiado de estado", "Estado")

            NotificationType.INCIDENT_RESOLVED ->
                Triple("Incidencia resuelta", "La incidencia ha sido resuelta", "Estado")

            NotificationType.MESSAGE_PUBLIC,
            NotificationType.MESSAGE_INTERNAL ->
                Triple(item.actorName ?: "Nuevo mensaje", "Han añadido un mensaje en la incidencia", "Mensaje")

            NotificationType.ATTACHMENT_UPLOADED ->
                Triple("Adjunto añadido", "Se ha añadido un archivo a la incidencia", "Adjunto")

            else ->
                Triple(item.actorName ?: "General", item.body, "General")
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
}