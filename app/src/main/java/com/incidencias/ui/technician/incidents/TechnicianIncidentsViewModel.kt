package com.incidencias.ui.technician.incidents

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.incidencias.data.remote.dto.incident.IncidentListItemResponse
import com.incidencias.data.repository.IncidentRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.OffsetDateTime

class TechnicianIncidentsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = IncidentRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(TechnicianIncidentsUiState())
    val uiState: StateFlow<TechnicianIncidentsUiState> = _uiState

    private val pageSize = 100

    fun loadIncidents(mode: TechnicianListMode, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = !forceRefresh,
                isRefreshing = forceRefresh,
                errorMessage = null,
                emptyMessage = null,
                mode = mode
            )

            try {
                val statusResults = mode.includedStatuses.map { status ->
                    async { fetchAllPagesForStatus(status) }
                }.awaitAll()

                val allItems = statusResults
                    .flatten()
                    .distinctBy { it.id }
                    .filter { item ->
                        when (mode) {
                            TechnicianListMode.TEAM_UNASSIGNED -> item.assignedTechnicianId == null
                            TechnicianListMode.MY_ASSIGNED ->
                                item.isAssignedToCurrentUser && item.statusName != "CLOSED"
                        }
                    }
                    .sortedWith(compareByDescending<IncidentListItemResponse> { parseCreatedAt(it.createdAt) }
                        .thenByDescending { it.id ?: 0 })

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    incidents = allItems,
                    errorMessage = null,
                    emptyMessage = if (allItems.isEmpty()) mode.emptyMessage else null,
                    mode = mode
                )
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = "No se pudo conectar con el servidor",
                    incidents = emptyList(),
                    emptyMessage = null,
                    mode = mode
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = e.message ?: "No se pudieron cargar las incidencias",
                    incidents = emptyList(),
                    emptyMessage = null,
                    mode = mode
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
            val response = repository.getIncidents(status = status, page = page, size = pageSize)

            if (!response.isSuccessful || response.body() == null) {
                throw IOException(
                    when (response.code()) {
                        401 -> "La sesión ha expirado"
                        403 -> "No tienes permiso para ver estas incidencias"
                        else -> "No se pudieron cargar las incidencias"
                    }
                )
            }

            val body = response.body()!!
            collected += body.content
            hasMore = body.page + 1 < body.totalPages
            page++
        }

        return collected
    }

    private fun parseCreatedAt(value: String?): OffsetDateTime? {
        return try {
            if (value.isNullOrBlank()) null else OffsetDateTime.parse(value)
        } catch (_: Exception) {
            null
        }
    }
}
