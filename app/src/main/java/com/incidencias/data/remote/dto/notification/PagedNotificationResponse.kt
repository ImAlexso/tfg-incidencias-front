package com.incidencias.data.remote.dto.notification

data class PagedNotificationResponse(
    val content: List<NotificationResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int
)