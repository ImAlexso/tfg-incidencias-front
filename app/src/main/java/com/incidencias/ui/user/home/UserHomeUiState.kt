package com.incidencias.ui.user.home

data class UserHomeUiState(
    val isLoading: Boolean = false,
    val firstName: String? = null,
    val activeIncidentsCount: Int = 0,
    val unreadNotificationsCount: Long = 0,
    val errorMessage: String? = null
)