package com.incidencias.ui.user.incidents.create

sealed class CreateIncidentUiState {
    data object Idle : CreateIncidentUiState()
    data object Loading : CreateIncidentUiState()
    data class Success(val referenceCode: String) : CreateIncidentUiState()
    data class Error(val message: String) : CreateIncidentUiState()
}
