package com.incidencias.ui.incident

import com.incidencias.data.remote.dto.catalog.PriorityResponse
import com.incidencias.data.remote.dto.catalog.TeamResponse
import com.incidencias.data.remote.dto.incident.AssignableTechnicianResponse

sealed interface IncidentDetailEvent {
    data class ShowMessage(val message: String) : IncidentDetailEvent
    data object CloseScreen : IncidentDetailEvent
    data class ShowPriorityPicker(val priorities: List<PriorityResponse>) : IncidentDetailEvent
    data class ShowTeamPicker(val teams: List<TeamResponse>) : IncidentDetailEvent
    data class ShowTechnicianPicker(val technicians: List<AssignableTechnicianResponse>) : IncidentDetailEvent
    data class OpenDownloadUrl(val url: String) : IncidentDetailEvent
}