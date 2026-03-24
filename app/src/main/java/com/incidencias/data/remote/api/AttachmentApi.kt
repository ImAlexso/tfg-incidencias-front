package com.incidencias.data.remote.api

import com.incidencias.data.remote.dto.attachment.AttachmentResponse
import com.incidencias.data.remote.dto.attachment.DownloadUrlResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface AttachmentApi {

    @GET("incidents/{incidentId}/attachments")
    suspend fun getAttachmentsByIncidentId(
        @Path("incidentId") incidentId: Long
    ): Response<List<AttachmentResponse>>

    @Multipart
    @POST("incidents/{incidentId}/attachments")
    suspend fun uploadAttachment(
        @Path("incidentId") incidentId: Long,
        @Part file: MultipartBody.Part,
        @Part("type") type: RequestBody
    ): Response<AttachmentResponse>

    @GET("incidents/{incidentId}/attachments/{attachmentId}/download-url")
    suspend fun getDownloadUrl(
        @Path("incidentId") incidentId: Long,
        @Path("attachmentId") attachmentId: Long
    ): Response<DownloadUrlResponse>

    @DELETE("incidents/{incidentId}/attachments/{attachmentId}")
    suspend fun deleteAttachment(
        @Path("incidentId") incidentId: Long,
        @Path("attachmentId") attachmentId: Long
    ): Response<Unit>
}