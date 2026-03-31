package com.incidencias.data.remote.api

import com.incidencias.data.remote.dto.incident.AssignTechnicianRequest
import com.incidencias.data.remote.dto.incident.AssignableTechnicianResponse
import com.incidencias.data.remote.dto.incident.CreateIncidentRequest
import com.incidencias.data.remote.dto.incident.IncidentDetailResponse
import com.incidencias.data.remote.dto.incident.IncidentResponse
import com.incidencias.data.remote.dto.incident.PagedIncidentResponse
import com.incidencias.data.remote.dto.incident.UpdateIncidentPriorityRequest
import com.incidencias.data.remote.dto.incident.UpdateIncidentStatusRequest
import com.incidencias.data.remote.dto.incident.UpdateIncidentTeamRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface IncidentApi {

    @POST("incidents")
    suspend fun createIncident(
        @Body request: CreateIncidentRequest
    ): Response<IncidentResponse>

    @GET("incidents")
    suspend fun getIncidents(
        @Query("status") status: String?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<PagedIncidentResponse>

    @GET("incidents/{id}/detail")
    suspend fun getIncidentDetail(
        @Path("id") id: Long
    ): Response<IncidentDetailResponse>

    @PATCH("incidents/{id}/status")
    suspend fun updateIncidentStatus(
        @Path("id") id: Long,
        @Body request: UpdateIncidentStatusRequest
    ): Response<Unit>

    @POST("incidents/{id}/resolve")
    suspend fun resolveIncident(
        @Path("id") id: Long
    ): Response<Unit>

    @PATCH("incidents/{id}/assign-technician")
    suspend fun assignTechnician(
        @Path("id") id: Long,
        @Body request: AssignTechnicianRequest
    ): Response<Unit>

    @PATCH("incidents/{id}/priority")
    suspend fun updateIncidentPriority(
        @Path("id") id: Long,
        @Body request: UpdateIncidentPriorityRequest
    ): Response<Unit>

    @PATCH("incidents/{id}/team")
    suspend fun updateIncidentTeam(
        @Path("id") id: Long,
        @Body request: UpdateIncidentTeamRequest
    ): Response<Unit>

    @POST("incidents/{id}/close")
    suspend fun closeIncident(
        @Path("id") id: Long
    ): Response<Unit>

    @GET("incidents/{id}/assignable-technicians")
    suspend fun getAssignableTechnicians(
        @Path("id") id: Long
    ): Response<List<AssignableTechnicianResponse>>
}