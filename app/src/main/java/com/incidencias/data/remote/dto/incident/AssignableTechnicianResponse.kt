package com.incidencias.data.remote.dto.incident

data class AssignableTechnicianResponse(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val teamId: Long?,
    val teamName: String?
)