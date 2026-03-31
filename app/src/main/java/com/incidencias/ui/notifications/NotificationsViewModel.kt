package com.incidencias.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.incidencias.data.remote.dto.notification.NotificationResponse
import com.incidencias.data.repository.NotificationRepository
import com.incidencias.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.IOException

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NotificationRepository(application.applicationContext)
    private val sessionManager = SessionManager(application.applicationContext)

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState

    private val pageSize = 20

    fun loadNotifications(forceRefresh: Boolean = false) {
        fetchNotifications(
            page = 0,
            append = false,
            isRefresh = forceRefresh
        )
    }

    fun loadMoreNotifications() {
        val state = _uiState.value
        if (state.isLoading || state.isRefreshing || state.isLoadingMore || !state.hasMorePages) return

        fetchNotifications(
            page = state.currentPage + 1,
            append = true,
            isRefresh = false
        )
    }

    fun setFilter(filter: NotificationFilter) {
        if (_uiState.value.selectedFilter == filter) return
        _uiState.value = NotificationsUiState(selectedFilter = filter)
        loadNotifications(forceRefresh = false)
    }

    fun markAsRead(notificationId: Long, onFinished: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val response = repository.markAsRead(notificationId)
                if (response.isSuccessful) {
                    loadNotifications(forceRefresh = true)
                    onFinished?.invoke()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = when (response.code()) {
                            401 -> "La sesión ha expirado"
                            403 -> "No tienes permiso para modificar esta notificación"
                            404 -> "La notificación no existe"
                            else -> "No se pudo marcar la notificación como leída"
                        }
                    )
                }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "No se pudo conectar con el servidor"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Error al marcar como leída"
                )
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val response = repository.markAllAsRead()
                if (response.isSuccessful) {
                    loadNotifications(forceRefresh = true)
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = when (response.code()) {
                            401 -> "La sesión ha expirado"
                            403 -> "No tienes permiso para modificar las notificaciones"
                            else -> "No se pudieron marcar todas como leídas"
                        }
                    )
                }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "No se pudo conectar con el servidor"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Error al marcar todas como leídas"
                )
            }
        }
    }

    fun deleteNotification(notificationId: Long) {
        viewModelScope.launch {
            try {
                val response = repository.deleteNotification(notificationId)
                if (response.isSuccessful) {
                    loadNotifications(forceRefresh = true)
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = when (response.code()) {
                            401 -> "La sesión ha expirado"
                            403 -> "No tienes permiso para borrar esta notificación"
                            404 -> "La notificación no existe"
                            else -> "No se pudo borrar la notificación"
                        }
                    )
                }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "No se pudo conectar con el servidor"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Error al borrar la notificación"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun fetchNotifications(page: Int, append: Boolean, isRefresh: Boolean) {
        val currentFilter = _uiState.value.selectedFilter

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = !append && !isRefresh,
                isRefreshing = isRefresh,
                isLoadingMore = append,
                errorMessage = null
            )

            try {
                val onlyUnread = when (currentFilter) {
                    NotificationFilter.UNREAD -> true
                    else -> null
                }

                val type = when (currentFilter) {
                    NotificationFilter.INCIDENT_CREATED -> "INCIDENT_CREATED"
                    NotificationFilter.STATUS_CHANGED -> "STATUS_CHANGED"
                    NotificationFilter.MESSAGE_PUBLIC -> "MESSAGE_PUBLIC"
                    NotificationFilter.ATTACHMENT_UPLOADED -> "ATTACHMENT_UPLOADED"
                    else -> null
                }

                val notificationsResponse = repository.getNotifications(
                    onlyUnread = onlyUnread,
                    type = type,
                    page = page,
                    size = pageSize
                )

                val unreadCountResponse = repository.getUnreadCount()

                if (!notificationsResponse.isSuccessful || notificationsResponse.body() == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = when (notificationsResponse.code()) {
                            401 -> "La sesión ha expirado"
                            403 -> "No tienes permiso para ver las notificaciones"
                            else -> "No se pudieron cargar las notificaciones"
                        }
                    )
                    return@launch
                }

                val currentUserId = sessionManager.userIdFlow.firstOrNull()

                val pageBody = notificationsResponse.body()!!

                val newItems = pageBody.content
                    .filter { notification ->
                        notification.actorId == null || notification.actorId != currentUserId
                    }
                    .map { it.toUiModel() }

                val mergedItems = if (append) {
                    _uiState.value.items + newItems
                } else {
                    newItems
                }

                val unreadCount = if (unreadCountResponse.isSuccessful && unreadCountResponse.body() != null) {
                    unreadCountResponse.body()!!.count
                } else {
                    _uiState.value.unreadCount
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    items = mergedItems,
                    unreadCount = unreadCount,
                    errorMessage = null,
                    currentPage = pageBody.page,
                    hasMorePages = pageBody.page + 1 < pageBody.totalPages
                )
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    errorMessage = "No se pudo conectar con el servidor"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    errorMessage = e.message ?: "Error al cargar notificaciones"
                )
            }
        }
    }

    private fun NotificationResponse.toUiModel(): NotificationUiModel {
        return NotificationUiModel(
            id = id,
            incidentId = incidentId,
            referenceCode = referenceCode,
            type = type.toNotificationType(),
            title = title,
            body = body,
            read = read,
            readAt = readAt,
            createdAt = createdAt,
            targetUrl = targetUrl,
            actorName = actorName
        )
    }

    private fun String.toNotificationType(): NotificationType {
        return try {
            NotificationType.valueOf(this)
        } catch (_: Exception) {
            NotificationType.UNKNOWN
        }
    }
}