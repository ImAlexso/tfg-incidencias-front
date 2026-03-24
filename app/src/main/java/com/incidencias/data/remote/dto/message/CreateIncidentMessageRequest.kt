package com.incidencias.data.remote.dto.message

data class CreateIncidentMessageRequest(
    val message: String,
    val internal: Boolean
)