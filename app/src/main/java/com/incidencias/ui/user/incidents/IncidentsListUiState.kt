package com.incidencias.ui.user.incidents

import com.incidencias.data.remote.dto.incident.IncidentListItemResponse

data class IncidentsListUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val incidents: List<IncidentListItemResponse> = emptyList(),
    val errorMessage: String? = null,
    val emptyMessage: String? = null,
    val currentPage: Int = -1,
    val hasMorePages: Boolean = true,
    val mode: IncidentListMode = IncidentListMode.ACTIVE
)