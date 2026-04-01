package com.incidencias.data.remote.dto.catalog

data class TeamTechnicianResponse(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val teamId: Long?,
    val teamName: String?
) {
    val fullName: String
        get() = listOf(firstName, lastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { email }
}