package com.incidencias.data.remote.dto.message

data class IncidentMessageResponse(
    val id: Long,
    val incidentId: Long,
    val authorId: Long,
    val authorName: String,
    val authorEmail: String,
    val authorRole: String,
    val message: String,
    val visibility: String,
    val createdAt: String
)