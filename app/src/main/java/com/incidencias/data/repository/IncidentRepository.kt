package com.incidencias.data.repository

import android.content.Context
import com.incidencias.data.remote.api.IncidentApi
import com.incidencias.data.remote.dto.incident.AssignTechnicianRequest
import com.incidencias.data.remote.dto.incident.AssignableTechnicianResponse
import com.incidencias.data.remote.dto.incident.CreateIncidentRequest
import com.incidencias.data.remote.dto.incident.IncidentDetailResponse
import com.incidencias.data.remote.dto.incident.IncidentResponse
import com.incidencias.data.remote.dto.incident.PagedIncidentResponse
import com.incidencias.data.remote.dto.incident.UpdateIncidentPriorityRequest
import com.incidencias.data.remote.dto.incident.UpdateIncidentStatusRequest
import com.incidencias.data.remote.dto.incident.UpdateIncidentTeamRequest
import com.incidencias.data.remote.retrofit.RetrofitClient
import retrofit2.Response

class IncidentRepository(context: Context) {

    private val incidentApi: IncidentApi =
        RetrofitClient.createService(context, IncidentApi::class.java)

    suspend fun createIncident(request: CreateIncidentRequest): Response<IncidentResponse> {
        return incidentApi.createIncident(request)
    }

    suspend fun getIncidents(
        status: String?,
        page: Int,
        size: Int
    ): Response<PagedIncidentResponse> {
        return incidentApi.getIncidents(status, page, size)
    }

    suspend fun getIncidentDetail(id: Long): Response<IncidentDetailResponse> {
        return incidentApi.getIncidentDetail(id)
    }

    suspend fun updateIncidentStatus(id: Long, statusId: Long): Response<Unit> {
        return incidentApi.updateIncidentStatus(id, UpdateIncidentStatusRequest(statusId))
    }

    suspend fun resolveIncident(id: Long): Response<Unit> {
        return incidentApi.resolveIncident(id)
    }

    suspend fun assignTechnician(id: Long, technicianId: Long): Response<Unit> {
        return incidentApi.assignTechnician(id, AssignTechnicianRequest(technicianId))
    }

    suspend fun updateIncidentPriority(id: Long, priorityId: Long): Response<Unit> {
        return incidentApi.updateIncidentPriority(id, UpdateIncidentPriorityRequest(priorityId))
    }

    suspend fun updateIncidentTeam(id: Long, teamId: Long): Response<Unit> {
        return incidentApi.updateIncidentTeam(id, UpdateIncidentTeamRequest(teamId))
    }

    suspend fun closeIncident(id: Long): Response<Unit> {
        return incidentApi.closeIncident(id)
    }

    suspend fun getAssignableTechnicians(id: Long): Response<List<AssignableTechnicianResponse>> {
        return incidentApi.getAssignableTechnicians(id)
    }
}