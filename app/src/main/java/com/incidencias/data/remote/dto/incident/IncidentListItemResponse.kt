package com.incidencias.data.remote.dto.incident

data class IncidentListItemResponse(
    val id: Long,
    val referenceCode: String,
    val title: String,
    val statusName: String,
    val priorityName: String?,
    val currentTeamName: String?
)