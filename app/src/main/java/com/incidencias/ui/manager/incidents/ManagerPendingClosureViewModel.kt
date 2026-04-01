package com.incidencias.ui.manager.incidents

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.incidencias.data.remote.dto.incident.IncidentListItemResponse
import com.incidencias.data.repository.IncidentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

data class ManagerPendingClosureUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val incidents: List<IncidentListItemResponse> = emptyList(),
    val errorMessage: String? = null,
    val emptyMessage: String? = null
)

class ManagerPendingClosureViewModel(application: Application) :
    AndroidViewModel(application) {

    private val repository = IncidentRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(ManagerPendingClosureUiState(isLoading = true))
    val uiState: StateFlow<ManagerPendingClosureUiState> = _uiState

    fun loadPendingClosure(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh) {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = true,
                    errorMessage = null
                )
            }

            try {
                val response = repository.getIncidents(
                    status = "RESOLVED",
                    page = 0,
                    size = 50
                )

                if (!response.isSuccessful || response.body() == null) {
                    throw IOException("No se pudieron cargar las incidencias resueltas")
                }

                val incidents = response.body()!!.content

                _uiState.value = ManagerPendingClosureUiState(
                    isLoading = false,
                    isRefreshing = false,
                    incidents = incidents,
                    emptyMessage = "No hay incidencias resueltas"
                )

            } catch (e: IOException) {
                _uiState.value = ManagerPendingClosureUiState(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = "No se pudieron cargar las incidencias resueltas"
                )
            } catch (e: Exception) {
                _uiState.value = ManagerPendingClosureUiState(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = e.message ?: "Error inesperado"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}