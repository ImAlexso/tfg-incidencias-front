package com.incidencias.ui.manager.assignment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.incidencias.data.remote.dto.incident.IncidentListItemResponse
import com.incidencias.data.remote.dto.catalog.TeamTechnicianResponse
import com.incidencias.data.repository.CatalogRepository
import com.incidencias.data.repository.IncidentRepository
import com.incidencias.session.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.IOException

data class ManagerAssignmentUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val unassignedIncidents: List<IncidentListItemResponse> = emptyList(),
    val technicians: List<TeamTechnicianResponse> = emptyList(),
    val errorMessage: String? = null
)

sealed class ManagerAssignmentEvent {
    data class ShowMessage(val message: String) : ManagerAssignmentEvent()
}

class ManagerAssignmentViewModel(application: Application) : AndroidViewModel(application) {

    private val incidentRepository = IncidentRepository(application.applicationContext)
    private val catalogRepository = CatalogRepository(application.applicationContext)
    private val sessionManager = SessionManager(application.applicationContext)

    private val _uiState = MutableStateFlow(ManagerAssignmentUiState(isLoading = true))
    val uiState: StateFlow<ManagerAssignmentUiState> = _uiState

    private val _events = MutableSharedFlow<ManagerAssignmentEvent>()
    val events: SharedFlow<ManagerAssignmentEvent> = _events

    fun loadBoard(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh) {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            } else {
                _uiState.value = _uiState.value.copy(isRefreshing = true, errorMessage = null)
            }

            try {
                val teamId = sessionManager.teamIdFlow.firstOrNull()
                    ?: throw IllegalStateException("No se encontró el equipo del manager")

                val incidentsResponse = incidentRepository.getIncidents("OPEN", 0, 100)
                val techniciansResponse = catalogRepository.getTechniciansByTeam(teamId)

                if (!incidentsResponse.isSuccessful || incidentsResponse.body() == null) {
                    throw IOException("No se pudieron cargar las incidencias")
                }

                if (!techniciansResponse.isSuccessful || techniciansResponse.body() == null) {
                    throw IOException("No se pudieron cargar los técnicos")
                }

                val unassigned = incidentsResponse.body()!!.content.filter { it.assignedTechnicianId == null }

                _uiState.value = ManagerAssignmentUiState(
                    isLoading = false,
                    isRefreshing = false,
                    unassignedIncidents = unassigned,
                    technicians = techniciansResponse.body().orEmpty(),
                    errorMessage = null
                )
            } catch (e: IOException) {
                _uiState.value = ManagerAssignmentUiState(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = "No se pudo cargar el reparto visual"
                )
            } catch (e: Exception) {
                _uiState.value = ManagerAssignmentUiState(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = e.message ?: "No se pudo cargar el reparto visual"
                )
            }
        }
    }

    fun assignIncident(incidentId: Long, technicianId: Long, technicianName: String) {
        viewModelScope.launch {
            try {
                val response = incidentRepository.assignTechnician(incidentId, technicianId)
                if (response.isSuccessful) {
                    _events.emit(ManagerAssignmentEvent.ShowMessage("Incidencia asignada a $technicianName"))
                    loadBoard(forceRefresh = true)
                } else {
                    _events.emit(ManagerAssignmentEvent.ShowMessage("No se pudo asignar la incidencia"))
                }
            } catch (e: Exception) {
                _events.emit(
                    ManagerAssignmentEvent.ShowMessage(
                        e.message ?: "No se pudo asignar la incidencia"
                    )
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}