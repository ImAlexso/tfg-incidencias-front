package com.incidencias.data.repository

import android.content.Context
import com.incidencias.data.remote.api.AttachmentApi
import com.incidencias.data.remote.dto.attachment.AttachmentResponse
import com.incidencias.data.remote.dto.attachment.DownloadUrlResponse
import com.incidencias.data.remote.retrofit.RetrofitClient
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response

class AttachmentRepository(context: Context) {

    private val attachmentApi: AttachmentApi =
        RetrofitClient.createService(context, AttachmentApi::class.java)

    suspend fun getAttachmentsByIncidentId(incidentId: Long): Response<List<AttachmentResponse>> {
        return attachmentApi.getAttachmentsByIncidentId(incidentId)
    }

    suspend fun uploadAttachment(
        incidentId: Long,
        file: MultipartBody.Part,
        type: RequestBody
    ): Response<AttachmentResponse> {
        return attachmentApi.uploadAttachment(incidentId, file, type)
    }

    suspend fun getDownloadUrl(
        incidentId: Long,
        attachmentId: Long
    ): Response<DownloadUrlResponse> {
        return attachmentApi.getDownloadUrl(incidentId, attachmentId)
    }
}