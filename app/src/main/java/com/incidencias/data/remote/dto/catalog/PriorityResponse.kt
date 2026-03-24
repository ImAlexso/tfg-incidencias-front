package com.incidencias.data.remote.dto.catalog

data class PriorityResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val active: Boolean,
    val level: Int
)