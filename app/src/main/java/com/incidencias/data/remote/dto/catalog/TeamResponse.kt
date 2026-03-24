package com.incidencias.data.remote.dto.catalog

data class TeamResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val active: Boolean
)