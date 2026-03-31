package com.incidencias.ui.technician.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.incidencias.data.remote.dto.incident.IncidentListItemResponse
import com.incidencias.data.repository.IncidentRepository
import com.incidencias.data.repository.NotificationRepository
import com.incidencias.session.SessionManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.IOException

class TechnicianHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val incidentRepository = IncidentRepository(application.applicationContext)
    private val notificationRepository = NotificationRepository(application.applicationContext)
    private val sessionManager = SessionManager(application.applicationContext)

    private val _uiState = MutableStateFlow(TechnicianHomeUiState(isLoading = true))
    val uiState: StateFlow<TechnicianHomeUiState> = _uiState

    private val pageSize = 100
    private val statuses = listOf("OPEN", "IN_PROGRESS", "RESOLVED")

    fun loadHomeData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh) {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = null)
            }

            try {
                val firstName = sessionManager.firstNameFlow.firstOrNull()

                val allItems = statuses.map { status ->
                    async { fetchAllPagesForStatus(status) }
                }.awaitAll().flatten().distinctBy { it.id }

                val teamQueueCount = allItems.count {
                    it.assignedTechnicianId == null && !it.statusName.equals("CLOSED", ignoreCase = true)
                }

                val myAssignedCount = allItems.count { it.isAssignedToCurrentUser }

                val unreadCount = try {
                    val response = notificationRepository.getUnreadCount()
                    if (response.isSuccessful && response.body() != null) {
                        response.body()!!.count
                    } else {
                        0L
                    }
                } catch (_: Exception) {
                    0L
                }

                _uiState.value = TechnicianHomeUiState(
                    isLoading = false,
                    firstName = firstName,
                    teamQueueCount = teamQueueCount,
                    myAssignedCount = myAssignedCount,
                    unreadNotificationsCount = unreadCount,
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