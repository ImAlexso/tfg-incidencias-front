package com.incidencias.ui.user.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.incidencias.data.repository.IncidentRepository
import com.incidencias.data.repository.NotificationRepository
import com.incidencias.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.IOException

class UserHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val incidentRepository = IncidentRepository(application.applicationContext)
    private val notificationRepository = NotificationRepository(application.applicationContext)
    private val sessionManager = SessionManager(application.applicationContext)

    private val _uiState = MutableStateFlow(UserHomeUiState())
    val uiState: StateFlow<UserHomeUiState> = _uiState

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
                val activeCount = fetchActiveIncidentsCount()

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

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    firstName = firstName,
                    activeIncidentsCount = activeCount,
                    unreadNotificationsCount = unreadCount,
                    errorMessage = null
                )
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No se pudo conectar con el servidor"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "No se pudo cargar la información de inicio"
                )
            }
        }
    }

    private suspend fun fetchActiveIncidentsCount(): Int {
        var page = 0
        var hasMore = true
        var activeCount = 0

        while (hasMore) {
            val response = incidentRepository.getIncidents(
                status = null,
                page = page,
                size = pageSize
            )

            if (!response.isSuccessful || response.body() == null) {
                throw IOException(
                    when (response.code()) {
                        401 -> "La sesión ha expirado"
                        403 -> "No tienes permiso para ver las incidencias"
                        else -> "No se pudieron cargar las incidencias"
                    }
                )
            }

            val body = response.body()!!
            activeCount += body.content.count { !it.statusName.equals("CLOSED", ignoreCase = true) }
            hasMore = body.page + 1 < body.totalPages
            page++
        }

        return activeCount
    }
}
