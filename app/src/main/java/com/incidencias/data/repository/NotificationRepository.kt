package com.incidencias.data.repository

import android.content.Context
import com.incidencias.data.remote.retrofit.RetrofitClient
import com.incidencias.data.remote.api.NotificationApi

class NotificationRepository(context: Context) {

    private val api = RetrofitClient.createService(context, NotificationApi::class.java)

    suspend fun getNotifications(
        onlyUnread: Boolean? = null,
        type: String? = null,
        page: Int = 0,
        size: Int = 20
    ) = api.getNotifications(
        onlyUnread = onlyUnread,
        type = type,
        page = page,
        size = size
    )

    suspend fun getUnreadCount() = api.getUnreadCount()

    suspend fun markAsRead(id: Long) = api.markAsRead(id)

    suspend fun markAllAsRead() = api.markAllAsRead()

    suspend fun deleteNotification(id: Long) = api.deleteNotification(id)
}
