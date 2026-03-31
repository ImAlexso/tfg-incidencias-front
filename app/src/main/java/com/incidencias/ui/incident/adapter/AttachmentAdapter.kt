package com.incidencias.ui.incident.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.incidencias.data.remote.dto.attachment.AttachmentResponse
import com.incidencias.databinding.ItemAttachmentBinding

class AttachmentAdapter(
    private var items: List<AttachmentResponse>,
    private val onClick: (AttachmentResponse) -> Unit
) : RecyclerView.Adapter<AttachmentAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAttachmentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAttachmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.binding.tvFileName.text = item.originalFileName
        holder.binding.tvType.text = "Tipo: ${item.attachmentType}"
        holder.binding.tvUploadedBy.text = "Subido por: ${item.uploadedByEmail}"

        holder.binding.root.setOnClickListener {
            onClick(item)
        }
    }

    fun updateData(newItems: List<AttachmentResponse>) {
        items = newItems
        notifyDataSetChanged()
    }
}