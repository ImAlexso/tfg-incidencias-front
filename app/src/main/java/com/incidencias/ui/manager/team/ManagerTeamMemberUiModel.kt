package com.incidencias.ui.manager.team

data class ManagerTeamMemberUiModel(
    val technicianId: Long,
    val technicianName: String,
    val technicianEmail: String,
    val totalCount: Int,
    val openCount: Int,
    val inProgressCount: Int,
    val resolvedCount: Int
)