package com.incidencias.ui.technician.incidents

import com.incidencias.data.remote.dto.incident.IncidentListItemResponse

data class TechnicianIncidentsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val incidents: List<IncidentListItemResponse> = emptyList(),
    val errorMessage: String? = null,
    val emptyMessage: String? = null,
    val mode: TechnicianListMode = TechnicianListMode.TEAM_UNASSIGNED
)
