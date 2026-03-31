package com.incidencias.data.remote.dto.incident

import com.incidencias.data.remote.dto.attachment.AttachmentResponse
import com.incidencias.data.remote.dto.message.IncidentMessageResponse

data class IncidentDetailResponse(
    val id: Long,
    val referenceCode: String,
    val title: String,
    val description: String,
    val createdByEmail: String?,
    val assignedTechnicianId: Long?,
    val assignedTechnicianEmail: String?,
    val currentTeamName: String?,
    val categoryName: String?,
    val priorityName: String?,
    val statusName: String,
    val resolvedAt: String?,
    val closedAt: String?,
    val canAssignToMe: Boolean,
    val canResolve: Boolean,
    val canClose: Boolean,
    val canChangePriority: Boolean,
    val canChangeTeam: Boolean,
    val canAssignTechnician: Boolean,
    val messages: List<IncidentMessageResponse>,
    val attachments: List<AttachmentResponse>,
    val events: List<IncidentEventResponse>
)