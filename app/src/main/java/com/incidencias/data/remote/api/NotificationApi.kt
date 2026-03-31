package com.incidencias.data.remote.api

import com.incidencias.data.remote.dto.notification.PagedNotificationResponse
import com.incidencias.data.remote.dto.notification.UnreadCountResponse
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApi {

    @GET("notifications")
    suspend fun getNotifications(
        @Query("onlyUnread") onlyUnread: Boolean? = null,
        @Query("type") type: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedNotificationResponse>

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): Response<UnreadCountResponse>

    @PATCH("notifications/{id}/read")
    suspend fun markAsRead(
        @Path("id") id: Long
    ): Response<Unit>

    @PATCH("notifications/read-all")
    suspend fun markAllAsRead(): Response<Unit>

    @DELETE("notifications/{id}")
    suspend fun deleteNotification(
        @Path("id") id: Long
    ): Response<Unit>
}