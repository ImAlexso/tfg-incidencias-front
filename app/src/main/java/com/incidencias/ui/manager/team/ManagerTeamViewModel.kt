package com.incidencias.ui.manager.team

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

data class ManagerTeamUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val items: List<ManagerTeamMemberUiModel> = emptyList(),
    val errorMessage: String? = null,
    val emptyMessage: String = "No hay técnicos disponibles en el equipo"
)

class ManagerTeamViewModel(application: Application) : AndroidViewModel(application) {

    private val incidentRepository = IncidentRepository(application.applicationContext)
    private val catalogRepository = CatalogRepository(application.applicationContext)
    private val sessionManager = SessionManager(application.applicationContext)

    private val _uiState = MutableStateFlow(ManagerTeamUiState(isLoading = true))
    val uiState: StateFlow<ManagerTeamUiState> = _uiState

    fun loadTeam(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh) {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            } else {
                _uiState.value = _uiState.value.copy(isRefreshing = true, errorMessage = null)
            }

            try {
                val teamId = sessionManager.teamIdFlow.firstOrNull()
                    ?: throw IllegalStateException("No se encontró el equipo del manager")

                val techniciansResponse = catalogRepository.getTechniciansByTeam(teamId)
                if (!techniciansResponse.isSuccessful || techniciansResponse.body() == null) {
                    throw IOException("No se pudieron cargar los técnicos del equipo")
                }

                val technicians = techniciansResponse.body().orEmpty()

                val allIncidents = listOf("OPEN", "IN_PROGRESS", "RESOLVED")
                    .map { status -> async { fetchAllPagesForStatus(status) } }
                    .awaitAll()
                    .flatten()
                    .distinctBy { it.id }

                val items = technicians.map { technician ->
                    val technicianIncidents = allIncidents.filter { it.assignedTechnicianId == technician.id }

                    ManagerTeamMemberUiModel(
                        technicianId = technician.id,
                        technicianName = technician.fullName,
                        technicianEmail = technician.email,
                        totalCount = technicianIncidents.size,
                        openCount = technicianIncidents.count { it.statusName.equals("OPEN", ignoreCase = true) },
                        inProgressCount = technicianIncidents.count { it.statusName.equals("IN_PROGRESS", ignoreCase = true) },
                        resolvedCount = technicianIncidents.count { it.statusName.equals("RESOLVED", ignoreCase = true) }
                    )
                }.sortedWith(
                    compareByDescending<ManagerTeamMemberUiModel> { it.totalCount }
                        .thenByDescending { it.inProgressCount }
                        .thenBy { it.technicianName.lowercase() }
                )

                _uiState.value = ManagerTeamUiState(
                    isLoading = false,
                    isRefreshing = false,
                    items = items
                )
            } catch (e: IOException) {
                _uiState.value = ManagerTeamUiState(
                    isLoading = false,
                    isRefreshing = false,
                    items = emptyList(),
                    errorMessage = "No se pudo cargar la vista del equipo"
                )
            } catch (e: Exception) {
                _uiState.value = ManagerTeamUiState(
                    isLoading = false,
                    isRefreshing = false,
                    items = emptyList(),
                    errorMessage = e.message ?: "No se pudo cargar la vista del equipo"
                )
            }
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
            val response = incidentRepository.getIncidents(status = status, page = page, size = 100)

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