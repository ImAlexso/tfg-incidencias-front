package com.incidencias.ui.manager.team

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.incidencias.databinding.ItemManagerTeamMemberBinding

class ManagerTeamAdapter(
    private val onItemClick: (ManagerTeamMemberUiModel) -> Unit
) : ListAdapter<ManagerTeamMemberUiModel, ManagerTeamAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(val binding: ItemManagerTeamMemberBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemManagerTeamMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        holder.binding.tvName.text = item.technicianName
        holder.binding.tvSubtitle.text = item.technicianEmail
        holder.binding.tvOpenCount.text = item.openCount.toString()
        holder.binding.tvInProgressCount.text = item.inProgressCount.toString()
        holder.binding.tvResolvedCount.text = item.resolvedCount.toString()
        holder.binding.tvTotalCount.text = item.totalCount.toString()

        holder.binding.root.setOnClickListener {
            onItemClick(item)
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ManagerTeamMemberUiModel>() {
            override fun areItemsTheSame(
                oldItem: ManagerTeamMemberUiModel,
                newItem: ManagerTeamMemberUiModel
            ): Boolean = oldItem.technicianId == newItem.technicianId

            override fun areContentsTheSame(
                oldItem: ManagerTeamMemberUiModel,
                newItem: ManagerTeamMemberUiModel
            ): Boolean = oldItem == newItem
        }
    }
}