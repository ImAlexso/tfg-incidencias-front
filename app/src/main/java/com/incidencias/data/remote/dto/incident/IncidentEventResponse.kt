package com.incidencias.data.remote.dto.incident

data class IncidentEventResponse(
    val id: Long,
    val incidentId: Long,
    val eventType: String,
    val eventDescription: String,
    val performedByEmail: String?,
    val createdAt: String
)
