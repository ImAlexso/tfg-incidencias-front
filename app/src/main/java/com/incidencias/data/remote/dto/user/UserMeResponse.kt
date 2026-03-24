package com.incidencias.data.remote.dto.user

data class UserMeResponse(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String,
    val teamId: Long?,
    val teamName: String?
)