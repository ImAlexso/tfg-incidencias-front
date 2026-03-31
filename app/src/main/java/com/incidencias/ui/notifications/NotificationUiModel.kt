package com.incidencias.ui.notifications

data class NotificationUiModel(
    val id: Long,
    val incidentId: Long?,
    val referenceCode: String?,
    val type: NotificationType,
    val title: String,
    val body: String,
    val read: Boolean,
    val readAt: String?,
    val createdAt: String,
    val targetUrl: String?,
    val actorName: String?
)