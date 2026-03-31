package com.incidencias.ui.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.incidencias.databinding.ItemNotificationBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class NotificationAdapter(
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

        val actorPrefix = item.actorName?.let { "$it · " } ?: ""

        holder.binding.tvTitle.text = actorPrefix + item.title
        holder.binding.tvBody.text = item.body
        holder.binding.tvReference.text = item.referenceCode ?: "Sin incidencia"
        holder.binding.tvDate.text = formatDate(item.createdAt)
        holder.binding.viewUnread.visibility = if (item.read) View.INVISIBLE else View.VISIBLE

        when (item.type) {
            NotificationType.INCIDENT_CREATED -> {
                holder.binding.ivIcon.setImageResource(android.R.drawable.ic_input_add)
                holder.binding.tvType.text = "Creada"
            }
            NotificationType.STATUS_CHANGED,
            NotificationType.INCIDENT_RESOLVED,
            NotificationType.INCIDENT_CLOSED -> {
                holder.binding.ivIcon.setImageResource(android.R.drawable.ic_popup_sync)
                holder.binding.tvType.text = "Estado"
            }
            NotificationType.MESSAGE_PUBLIC,
            NotificationType.MESSAGE_INTERNAL -> {
                holder.binding.ivIcon.setImageResource(android.R.drawable.ic_dialog_email)
                holder.binding.tvType.text = "Mensaje"
            }
            NotificationType.ATTACHMENT_UPLOADED -> {
                holder.binding.ivIcon.setImageResource(android.R.drawable.ic_menu_upload)
                holder.binding.tvType.text = "Adjunto"
            }
            NotificationType.PRIORITY_CHANGED -> {
                holder.binding.ivIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                holder.binding.tvType.text = "Prioridad"
            }
            NotificationType.TEAM_CHANGED,
            NotificationType.TECHNICIAN_ASSIGNED,
            NotificationType.ASSIGNEE_REMOVED -> {
                holder.binding.ivIcon.setImageResource(android.R.drawable.ic_menu_manage)
                holder.binding.tvType.text = "Equipo"
            }
            NotificationType.UNKNOWN -> {
                holder.binding.ivIcon.setImageResource(android.R.drawable.ic_dialog_info)
                holder.binding.tvType.text = "General"
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
        return try {
            val parsed = LocalDateTime.parse(raw)
            parsed.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
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
}
