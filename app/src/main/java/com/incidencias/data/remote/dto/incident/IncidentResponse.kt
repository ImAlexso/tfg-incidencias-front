package com.incidencias.data.remote.dto.incident

data class IncidentResponse(
    val id: Long,
    val referenceCode: String,
    val title: String,
    val description: String,
    val createdByEmail: String,
    val assignedTechnicianEmail: String?,
    val currentTeamName: String?,
    val categoryName: String?,
    val priorityName: String?,
    val statusName: String
)