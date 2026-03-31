package com.incidencias.ui.technician.home

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

class TechnicianHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = IncidentRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(TechnicianHomeUiState(isLoading = true))
    val uiState: StateFlow<TechnicianHomeUiState> = _uiState

    private val pageSize = 100
    private val statuses = listOf("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED")

    fun loadCounts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val allItems = statuses.map { status ->
                    async { fetchAllPagesForStatus(status) }
                }.awaitAll().flatten().distinctBy { it.id }

                val teamQueueCount = allItems.count {
                    it.assignedTechnicianId == null && !it.statusName.equals("CLOSED", ignoreCase = true)
                }
                val myAssignedCount = allItems.count { it.isAssignedToCurrentUser }

                _uiState.value = TechnicianHomeUiState(
                    isLoading = false,
                    teamQueueCount = teamQueueCount,
                    myAssignedCount = myAssignedCount,
                    errorMessage = null
                )
            } catch (e: IOException) {
                _uiState.value = TechnicianHomeUiState(
                    isLoading = false,
                    errorMessage = "No se pudieron actualizar los contadores"
                )
            } catch (e: Exception) {
                _uiState.value = TechnicianHomeUiState(
                    isLoading = false,
                    errorMessage = e.message ?: "No se pudieron actualizar los contadores"
                )
            }
        }
    }

    private suspend fun fetchAllPagesForStatus(status: String): List<IncidentListItemResponse> {
        val collected = mutableListOf<IncidentListItemResponse>()
        var page = 0
        var hasMore = true

        while (hasMore) {
            val response = repository.getIncidents(status = status, page = page, size = pageSize)

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
