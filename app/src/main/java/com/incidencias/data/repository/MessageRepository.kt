package com.incidencias.data.repository

import android.content.Context
import com.incidencias.data.remote.api.MessageApi
import com.incidencias.data.remote.dto.message.CreateIncidentMessageRequest
import com.incidencias.data.remote.dto.message.IncidentMessageResponse
import com.incidencias.data.remote.retrofit.RetrofitClient
import retrofit2.Response

class MessageRepository(context: Context) {

    private val messageApi: MessageApi =
        RetrofitClient.createService(context, MessageApi::class.java)

    suspend fun getMessagesByIncidentId(incidentId: Long): Response<List<IncidentMessageResponse>> {
        return messageApi.getMessagesByIncidentId(incidentId)
    }

    suspend fun addPublicMessage(
        incidentId: Long,
        message: String
    ): Response<IncidentMessageResponse> {
        return messageApi.addMessage(
            incidentId = incidentId,
            request = CreateIncidentMessageRequest(
                message = message,
                internal = false
            )
        )
    }

    suspend fun addInternalMessage(
        incidentId: Long,
        message: String
    ): Response<IncidentMessageResponse> {
        return messageApi.addMessage(
            incidentId = incidentId,
            request = CreateIncidentMessageRequest(
                message = message,
                internal = true
            )
        )
    }
}