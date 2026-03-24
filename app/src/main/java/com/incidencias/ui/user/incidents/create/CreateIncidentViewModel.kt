package com.incidencias.ui.user.incidents.create

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.incidencias.data.remote.dto.catalog.CategoryResponse
import com.incidencias.data.remote.dto.catalog.PriorityResponse
import com.incidencias.data.remote.dto.catalog.TeamResponse
import com.incidencias.data.remote.dto.incident.CreateIncidentRequest
import com.incidencias.data.repository.CatalogRepository
import com.incidencias.data.repository.IncidentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CreateIncidentViewModel(application: Application) : AndroidViewModel(application) {

    private val catalogRepository = CatalogRepository(application.applicationContext)
    private val incidentRepository = IncidentRepository(application.applicationContext)

    private val _uiState = MutableStateFlow<CreateIncidentUiState>(CreateIncidentUiState.Idle)
    val uiState: StateFlow<CreateIncidentUiState> = _uiState

    private val _categories = MutableStateFlow<List<CategoryResponse>>(emptyList())
    val categories: StateFlow<List<CategoryResponse>> = _categories

    private val _priorities = MutableStateFlow<List<PriorityResponse>>(emptyList())
    val priorities: StateFlow<List<PriorityResponse>> = _priorities

    private val _teams = MutableStateFlow<List<TeamResponse>>(emptyList())
    val teams: StateFlow<List<TeamResponse>> = _teams

    fun loadCatalogs() {
        viewModelScope.launch {
            try {
                val categoriesResponse = catalogRepository.getCategories()
                val prioritiesResponse = catalogRepository.getPriorities()
                val teamsResponse = catalogRepository.getTeams()

                if (categoriesResponse.isSuccessful) {
                    _categories.value = categoriesResponse.body().orEmpty().filter { it.active }
                }

                if (prioritiesResponse.isSuccessful) {
                    _priorities.value = prioritiesResponse.body().orEmpty().filter { it.active }
                }

                if (teamsResponse.isSuccessful) {
                    _teams.value = teamsResponse.body().orEmpty().filter { it.active }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun createIncident(
        useAi: Boolean,
        title: String,
        description: String,
        categoryId: Long?,
        priorityId: Long?,
        teamId: Long?
    ) {
        if (title.isBlank() || description.isBlank()) {
            _uiState.value = CreateIncidentUiState.Error("Título y descripción son obligatorios")
            return
        }

        if (!useAi && (categoryId == null || priorityId == null || teamId == null)) {
            _uiState.value = CreateIncidentUiState.Error("Debes seleccionar categoría, prioridad y equipo")
            return
        }

        viewModelScope.launch {
            _uiState.value = CreateIncidentUiState.Loading

            try {
                val request = CreateIncidentRequest(
                    useAi = useAi,
                    title = title,
                    description = description,
                    categoryId = if (useAi) null else categoryId,
                    priorityId = if (useAi) null else priorityId,
                    teamId = if (useAi) null else teamId
                )

                val response = incidentRepository.createIncident(request)

                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = CreateIncidentUiState.Success(response.body()!!.referenceCode)
                } else {
                    _uiState.value = CreateIncidentUiState.Error("No se pudo crear la incidencia")
                }
            } catch (e: Exception) {
                _uiState.value = CreateIncidentUiState.Error(
                    e.message ?: "Error inesperado al crear la incidencia"
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = CreateIncidentUiState.Idle
    }
}