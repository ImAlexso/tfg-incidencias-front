package com.incidencias.ui.manager.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.incidencias.data.remote.dto.incident.IncidentListItemResponse
import com.incidencias.data.repository.CatalogRepository
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

class ManagerHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val incidentRepository = IncidentRepository(application.applicationContext)
    private val catalogRepository = CatalogRepository(application.applicationContext)
    private val notificationRepository = NotificationRepository(application.applicationContext)
    private val sessionManager = SessionManager(application.applicationContext)

    private val _uiState = MutableStateFlow(ManagerHomeUiState(isLoading = true))
    val uiState: StateFlow<ManagerHomeUiState> = _uiState

    private val pageSize = 100

    fun loadHomeData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh) {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = null)
            }

            try {
                val firstName = sessionManager.firstNameFlow.firstOrNull()
                val teamId = sessionManager.teamIdFlow.firstOrNull()

                val allItems = listOf("OPEN", "IN_PROGRESS", "RESOLVED")
                    .map { status -> async { fetchAllPagesForStatus(status) } }
                    .awaitAll()
                    .flatten()
                    .distinctBy { it.id }

                val openItems = allItems.filter { it.statusName.equals("OPEN", ignoreCase = true) }
                val inProgressItems = allItems.filter { it.statusName.equals("IN_PROGRESS", ignoreCase = true) }
                val resolvedItems = allItems.filter { it.statusName.equals("RESOLVED", ignoreCase = true) }

                val unassignedCount = openItems.count { it.assignedTechnicianId == null }
                val criticalCount = allItems.count { it.priorityName.equals("CRITICAL", ignoreCase = true) }

                val teamMembersCount = if (teamId != null) {
                    val response = catalogRepository.getTechniciansByTeam(teamId)
                    if (response.isSuccessful) {
                        response.body().orEmpty().size
                    } else {
                        0
                    }
                } else {
                    0
                }

                val unreadNotificationsCount = try {
                    val unreadResponse = notificationRepository.getUnreadCount()
                    if (unreadResponse.isSuccessful && unreadResponse.body() != null) {
                        unreadResponse.body()!!.count
                    } else {
                        0L
                    }
                } catch (_: Exception) {
                    0L
                }

                _uiState.value = ManagerHomeUiState(
                    isLoading = false,
                    firstName = firstName,
                    unassignedCount = unassignedCount,
                    resolvedCount = resolvedItems.size,
                    criticalCount = criticalCount,
                    activeIncidentsCount = openItems.size + inProgressItems.size,
                    teamMembersCount = teamMembersCount,
                    unreadNotificationsCount = unreadNotificationsCount,
                    errorMessage = null
                )
            } catch (e: IOException) {
                _uiState.value = ManagerHomeUiState(
                    isLoading = false,
                    errorMessage = "No se pudieron cargar los datos del portal manager"
                )
            } catch (e: Exception) {
                _uiState.value = ManagerHomeUiState(
                    isLoading = false,
                    errorMessage = e.message ?: "No se pudieron cargar los datos del portal manager"
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