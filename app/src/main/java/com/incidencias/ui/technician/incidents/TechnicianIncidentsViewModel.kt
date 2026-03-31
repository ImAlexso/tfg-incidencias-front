package com.incidencias.ui.technician.incidents

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.incidencias.data.remote.dto.incident.IncidentListItemResponse
import com.incidencias.data.repository.IncidentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class TechnicianIncidentsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = IncidentRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(TechnicianIncidentsUiState())
    val uiState: StateFlow<TechnicianIncidentsUiState> = _uiState

    private var currentMode: TechnicianListMode? = null

    private val pageSize = 100
    private val statuses = listOf("OPEN", "IN_PROGRESS", "RESOLVED")

    fun loadIncidents(mode: TechnicianListMode, forceRefresh: Boolean = false) {
        currentMode = mode

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = !forceRefresh,
                isRefreshing = forceRefresh,
                errorMessage = null
            )

            try {
                val allItems = mutableListOf<IncidentListItemResponse>()

                for (status in statuses) {
                    var page = 0
                    var hasMore = true

                    while (hasMore) {
                        val response = repository.getIncidents(
                            status = status,
                            page = page,
                            size = pageSize
                        )

                        if (!response.isSuccessful || response.body() == null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isRefreshing = false,
                                incidents = emptyList(),
                                emptyMessage = null,
                                errorMessage = when (response.code()) {
                                    401 -> "La sesión ha expirado"
                                    403 -> when (mode) {
                                        TechnicianListMode.MY_ASSIGNED ->
                                            "No tienes permiso para ver tus incidencias asignadas"
                                        TechnicianListMode.TEAM_UNASSIGNED ->
                                            "No tienes permiso para ver las pendientes del equipo"
                                    }

                                    else -> buildGenericErrorMessage(mode)
                                }
                            )
                            return@launch
                        }

                        val body = response.body()!!
                        allItems += body.content
                        hasMore = body.page + 1 < body.totalPages
                        page++
                    }
                }

                val filteredItems = when (mode) {
                    TechnicianListMode.MY_ASSIGNED -> {
                        allItems
                            .distinctBy { it.id }
                            .filter { it.isAssignedToCurrentUser }
                    }

                    TechnicianListMode.TEAM_UNASSIGNED -> {
                        allItems
                            .distinctBy { it.id }
                            .filter { it.assignedTechnicianId == null }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    incidents = filteredItems,
                    emptyMessage = buildEmptyMessage(mode, filteredItems),
                    errorMessage = null
                )
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    incidents = emptyList(),
                    emptyMessage = null,
                    errorMessage = "No se pudo conectar con el servidor"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    incidents = emptyList(),
                    emptyMessage = null,
                    errorMessage = e.message ?: buildGenericErrorMessage(mode)
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun buildEmptyMessage(
        mode: TechnicianListMode,
        items: List<IncidentListItemResponse>
    ): String? {
        if (items.isNotEmpty()) return null

        return when (mode) {
            TechnicianListMode.MY_ASSIGNED ->
                "No tienes incidencias asignadas en este momento"

            TechnicianListMode.TEAM_UNASSIGNED ->
                "No hay incidencias pendientes de asignación en tu equipo"
        }
    }

    private fun buildGenericErrorMessage(mode: TechnicianListMode?): String {
        return when (mode) {
            TechnicianListMode.MY_ASSIGNED ->
                "No se pudieron cargar tus incidencias asignadas"

            TechnicianListMode.TEAM_UNASSIGNED ->
                "No se pudieron cargar las incidencias pendientes del equipo"

            null ->
                "No se pudieron cargar las incidencias"
        }
    }
}