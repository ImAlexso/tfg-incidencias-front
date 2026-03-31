package com.incidencias.ui.notifications

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val items: List<NotificationUiModel> = emptyList(),
    val selectedFilter: NotificationFilter = NotificationFilter.ALL,
    val unreadCount: Long = 0,
    val errorMessage: String? = null,
    val currentPage: Int = 0,
    val hasMorePages: Boolean = true
)