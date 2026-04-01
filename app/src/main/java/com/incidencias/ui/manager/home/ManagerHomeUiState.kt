package com.incidencias.ui.manager.home

data class ManagerHomeUiState(
    val isLoading: Boolean = false,
    val firstName: String? = null,
    val unassignedCount: Int = 0,
    val resolvedCount: Int = 0,
    val criticalCount: Int = 0,
    val activeIncidentsCount: Int = 0,
    val teamMembersCount: Int = 0,
    val unreadNotificationsCount: Long = 0,
    val errorMessage: String? = null
)