package com.incidencias.ui.technician.home

data class TechnicianHomeUiState(
    val isLoading: Boolean = false,
    val teamQueueCount: Int = 0,
    val myAssignedCount: Int = 0,
    val errorMessage: String? = null
)
