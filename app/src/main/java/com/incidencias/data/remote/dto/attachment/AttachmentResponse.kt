package com.incidencias.data.remote.dto.attachment

data class AttachmentResponse(
    val id: Long,
    val incidentId: Long,
    val originalFileName: String,
    val contentType: String?,
    val fileSize: Long,
    val uploadedByEmail: String,
    val attachmentType: String,
    val createdAt: String
)
