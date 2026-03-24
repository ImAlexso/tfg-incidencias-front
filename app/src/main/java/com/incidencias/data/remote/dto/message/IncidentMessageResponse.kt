package com.incidencias.data.remote.dto.message

data class IncidentMessageResponse(
    val id: Long,
    val incidentId: Long,
    val authorEmail: String,
    val message: String,
    val visibility: String,
    val createdAt: String
)