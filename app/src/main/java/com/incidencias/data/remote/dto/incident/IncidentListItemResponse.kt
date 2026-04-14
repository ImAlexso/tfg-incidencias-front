package com.incidencias.data.remote.dto.incident

data class IncidentListItemResponse(
    val id: Long,
    val referenceCode: String,
    val title: String,
    val statusName: String,
    val priorityName: String?,
    val currentTeamName: String?,
    val assignedTechnicianId: Long?,
    val assignedTechnicianEmail: String?,
    val assignedTechnicianName: String?,
    val isAssignedToCurrentUser: Boolean,
    val canAssignToMe: Boolean,
    val createdAt: String
)