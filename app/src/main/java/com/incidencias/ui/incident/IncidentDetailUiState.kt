package com.incidencias.ui.incident

import com.incidencias.data.remote.dto.incident.IncidentDetailResponse

data class IncidentDetailUiState(
    val incidentId: Long = -1L,
    val role: String = "",
    val userId: Long? = null,
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val isSendingPublicMessage: Boolean = false,
    val isSendingInternalMessage: Boolean = false,
    val isUploadingAttachment: Boolean = false,
    val detail: IncidentDetailResponse? = null,
    val errorMessage: String? = null
) {
    val isClosed: Boolean
        get() = detail?.statusName.equals("CLOSED", ignoreCase = true)

    val isResolved: Boolean
        get() = detail?.statusName.equals("RESOLVED", ignoreCase = true)

    val isOpen: Boolean
        get() = detail?.statusName.equals("OPEN", ignoreCase = true)

    val isTechnician: Boolean
        get() = role == "TECHNICIAN"

    val isManagerOrAdmin: Boolean
        get() = role == "MANAGER" || role == "ADMIN"
}