package com.incidencias.ui.user.incidents

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.incidencias.data.remote.dto.incident.IncidentListItemResponse
import com.incidencias.data.repository.IncidentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class IncidentsListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = IncidentRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(IncidentsListUiState())
    val uiState: StateFlow<IncidentsListUiState> = _uiState

    private val pageSize = 20

    fun loadIncidents(mode: IncidentListMode, forceRefresh: Boolean = false) {
        _uiState.value = _uiState.value.copy(mode = mode)
        fetchPage(
            mode = mode,
            startPage = 0,
            append = false,
            isRefresh = forceRefresh
        )
    }

    fun loadMoreIncidents() {
        val state = _uiState.value
        if (state.isLoading || state.isRefreshing || state.isLoadingMore || !state.hasMorePages) return

        fetchPage(
            mode = state.mode,
            startPage = state.currentPage + 1,
            append = true,
            isRefresh = false
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun fetchPage(
        mode: IncidentListMode,
        startPage: Int,
        append: Boolean,
        isRefresh: Boolean
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = !append && !isRefresh,
                isRefreshing = isRefresh,
                isLoadingMore = append,
                errorMessage = null,
                emptyMessage = null,
                mode = mode
            )

            try {
                var pageToLoad = startPage
                var filteredBatch = emptyList<IncidentListItemResponse>()
                var lastProcessedPage = startPage
                var hasMorePages = false

                do {
                    val response = repository.getIncidents(
                        status = null,
                        page = pageToLoad,
                        size = pageSize
                    )

                    if (!response.isSuccessful || response.body() == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isLoadingMore = false,
                            errorMessage = when (response.code()) {
                                401 -> "La sesión ha expirado"
                                403 -> "No tienes permiso para ver las incidencias"
                                else -> "No se pudieron cargar las incidencias"
                            }
                        )
                        return@launch
                    }

                    val body = response.body()!!
                    val pageItems = body.content

                    filteredBatch = when (mode) {
                        IncidentListMode.ACTIVE -> {
                            pageItems.filter { !it.statusName.equals("CLOSED", ignoreCase = true) }
                        }
                        IncidentListMode.HISTORY -> {
                            pageItems.filter { it.statusName.equals("CLOSED", ignoreCase = true) }
                        }
                    }

                    lastProcessedPage = body.page
                    hasMorePages = body.page + 1 < body.totalPages
                    pageToLoad++
                } while (filteredBatch.isEmpty() && hasMorePages)

                val mergedItems = if (append) {
                    _uiState.value.incidents + filteredBatch
                } else {
                    filteredBatch
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    incidents = mergedItems,
                    errorMessage = null,
                    emptyMessage = if (mergedItems.isEmpty() && !hasMorePages) mode.emptyMessage else null,
                    currentPage = lastProcessedPage,
                    hasMorePages = hasMorePages
                )
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    errorMessage = "No se pudo conectar con el servidor"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    errorMessage = e.message ?: "Error al cargar incidencias"
                )
            }
        }
    }
}