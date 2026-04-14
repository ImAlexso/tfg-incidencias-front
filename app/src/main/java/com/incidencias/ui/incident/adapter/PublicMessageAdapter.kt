package com.incidencias.ui.incident.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.incidencias.R
import com.incidencias.data.remote.dto.message.IncidentMessageResponse
import com.incidencias.databinding.ItemPublicMessageBinding
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class PublicMessageAdapter(
    private val currentUserEmail: String,
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
        val context = holder.binding.root.context

        holder.binding.tvAuthor.text = buildAuthorLabel(item)
        holder.binding.tvCreatedAt.text = formatDate(item.createdAt)
        holder.binding.tvMessage.text = item.message

        when (item.authorRole?.uppercase().orEmpty()) {
            "TECHNICIAN" -> {
                holder.binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.badge_message_soft)
                )
                holder.binding.root.strokeColor =
                    ContextCompat.getColor(context, R.color.stroke_soft)
                holder.binding.ivRoleIcon.setImageResource(R.drawable.ic_role_support_small)
                holder.binding.ivRoleIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.success)
                )
            }

            "MANAGER", "ADMIN" -> {
                holder.binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.badge_internal_soft)
                )
                holder.binding.root.strokeColor =
                    ContextCompat.getColor(context, R.color.stroke_soft)
                holder.binding.ivRoleIcon.setImageResource(R.drawable.ic_role_manager_small)
                holder.binding.ivRoleIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.badge_internal)
                )
            }

            else -> {
                holder.binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.primary_container)
                )
                holder.binding.root.strokeColor =
                    ContextCompat.getColor(context, R.color.stroke_soft)
                holder.binding.ivRoleIcon.setImageResource(R.drawable.ic_role_user_small)
                holder.binding.ivRoleIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.primary)
                )
            }
        }
    }

    fun updateData(newItems: List<IncidentMessageResponse>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun buildAuthorLabel(item: IncidentMessageResponse): String {
        return if (item.authorEmail.equals(currentUserEmail, ignoreCase = true)) {
            "Tú"
        } else {
            when (item.authorRole?.uppercase()) {
                "TECHNICIAN" -> "Técnico: ${item.authorName}"
                "MANAGER" -> "Manager: ${item.authorName}"
                "ADMIN" -> "Admin: ${item.authorName}"
                "USER" -> "Usuario: ${item.authorName}"
                else -> item.authorName
            }
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