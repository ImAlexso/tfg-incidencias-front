package com.incidencias.ui.manager.incidents

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.incidencias.data.remote.dto.incident.IncidentListItemResponse
import com.incidencias.data.repository.CatalogRepository
import com.incidencias.data.repository.IncidentRepository
import com.incidencias.session.SessionManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.IOException

class ManagerIncidentsViewModel(application: Application) : AndroidViewModel(application) {

    private val incidentRepository = IncidentRepository(application.applicationContext)
    private val catalogRepository = CatalogRepository(application.applicationContext)
    private val sessionManager = SessionManager(application.applicationContext)

    private val _uiState = MutableStateFlow(ManagerIncidentsUiState(isLoading = true))
    val uiState: StateFlow<ManagerIncidentsUiState> = _uiState

    private val pageSize = 100

    fun loadActiveIncidents(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh) {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    isRefreshing = false,
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = true,
                    errorMessage = null
                )
            }

            try {
                val teamId = sessionManager.teamIdFlow.firstOrNull()

                val incidentResults = listOf("OPEN", "IN_PROGRESS").map { status ->
                    async { fetchAllPagesForStatus(status) }
                }.awaitAll().flatten().distinctBy { it.id }

                val technicians = if (teamId != null) {
                    val techniciansResponse = catalogRepository.getTechniciansByTeam(teamId)
                    if (techniciansResponse.isSuccessful) {
                        techniciansResponse.body().orEmpty()
                    } else {
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    incidents = incidentResults.sortedByDescending { it.createdAt },
                    technicians = technicians,
                    errorMessage = null,
                    emptyMessage = "No hay incidencias activas del equipo"
                )
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    incidents = emptyList(),
                    errorMessage = "No se pudieron cargar las incidencias del equipo",
                    emptyMessage = "No hay incidencias activas del equipo"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    incidents = emptyList(),
                    errorMessage = e.message ?: "No se pudieron cargar las incidencias del equipo",
                    emptyMessage = "No hay incidencias activas del equipo"
                )
            }
        }
    }

    fun updateFilters(
        status: String?,
        priority: String?,
        technicianId: Long?,
        technicianName: String?,
        onlyUnassigned: Boolean
    ) {
        _uiState.value = _uiState.value.copy(
            selectedStatus = status,
            selectedPriority = priority,
            selectedTechnicianId = technicianId,
            selectedTechnicianName = technicianName,
            onlyUnassigned = onlyUnassigned
        )
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            selectedStatus = null,
            selectedPriority = null,
            selectedTechnicianId = null,
            selectedTechnicianName = null,
            onlyUnassigned = false
        )
    }

    fun getFilteredIncidents(): List<IncidentListItemResponse> {
        val state = _uiState.value

        return state.incidents.filter { incident ->
            val statusMatch = state.selectedStatus == null ||
                    incident.statusName.equals(state.selectedStatus, ignoreCase = true)

            val priorityMatch = state.selectedPriority == null ||
                    incident.priorityName.equals(state.selectedPriority, ignoreCase = true)

            val technicianMatch = state.selectedTechnicianId == null ||
                    incident.assignedTechnicianId == state.selectedTechnicianId

            val unassignedMatch = !state.onlyUnassigned || incident.assignedTechnicianId == null

            statusMatch && priorityMatch && technicianMatch && unassignedMatch
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private suspend fun fetchAllPagesForStatus(status: String): List<IncidentListItemResponse> {
        val collected = mutableListOf<IncidentListItemResponse>()
        var page = 0
        var hasMore = true

        while (hasMore) {
            val response = incidentRepository.getIncidents(status = status, page = page, size = pageSize)

            if (!response.isSuccessful || response.body() == null) {
                throw IOException("No se pudieron cargar las incidencias")
            }

            val body = response.body()!!
            collected += body.content
            hasMore = body.page + 1 < body.totalPages
            page++
        }

        return collected
    }
}