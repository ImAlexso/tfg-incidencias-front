package com.incidencias.data.remote.dto.notification

data class NotificationResponse(
    val id: Long,
    val incidentId: Long?,
    val referenceCode: String?,
    val type: String,
    val title: String,
    val body: String,
    val read: Boolean,
    val readAt: String?,
    val createdAt: String,
    val targetUrl: String?,
    val actorId: Long?,
    val actorEmail: String?,
    val actorName: String?
)