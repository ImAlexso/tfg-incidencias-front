package com.incidencias.data.remote.dto.incident

data class CreateIncidentRequest(
    val useAi: Boolean,
    val title: String,
    val description: String,
    val categoryId: Long? = null,
    val priorityId: Long? = null,
    val teamId: Long? = null
)