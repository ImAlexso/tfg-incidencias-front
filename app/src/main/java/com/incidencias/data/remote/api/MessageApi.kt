package com.incidencias.data.remote.api

import com.incidencias.data.remote.dto.message.CreateIncidentMessageRequest
import com.incidencias.data.remote.dto.message.IncidentMessageResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MessageApi {

    @GET("incidents/{incidentId}/messages")
    suspend fun getMessagesByIncidentId(
        @Path("incidentId") incidentId: Long
    ): Response<List<IncidentMessageResponse>>

    @POST("incidents/{incidentId}/messages")
    suspend fun addMessage(
        @Path("incidentId") incidentId: Long,
        @Body request: CreateIncidentMessageRequest
    ): Response<IncidentMessageResponse>
}