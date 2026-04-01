package com.incidencias.ui.manager.incidents

import com.incidencias.data.remote.dto.catalog.TeamTechnicianResponse
import com.incidencias.data.remote.dto.incident.IncidentListItemResponse

data class ManagerIncidentsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val incidents: List<IncidentListItemResponse> = emptyList(),
    val technicians: List<TeamTechnicianResponse> = emptyList(),
    val selectedStatus: String? = null,
    val selectedPriority: String? = null,
    val selectedTechnicianId: Long? = null,
    val selectedTechnicianName: String? = null,
    val onlyUnassigned: Boolean = false,
    val errorMessage: String? = null,
    val emptyMessage: String? = null
)