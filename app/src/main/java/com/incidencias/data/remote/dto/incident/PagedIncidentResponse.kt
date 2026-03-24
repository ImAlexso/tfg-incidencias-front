package com.incidencias.data.remote.dto.incident

data class PagedIncidentResponse(
    val content: List<IncidentListItemResponse>,
    val totalElements: Int,
    val totalPages: Int,
    val page: Int,
    val size: Int
)