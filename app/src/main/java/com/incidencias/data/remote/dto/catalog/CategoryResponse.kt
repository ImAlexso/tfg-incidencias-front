package com.incidencias.data.remote.dto.catalog

data class CategoryResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val active: Boolean
)