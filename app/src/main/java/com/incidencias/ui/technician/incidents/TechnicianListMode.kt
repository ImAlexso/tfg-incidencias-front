package com.incidencias.ui.technician.incidents

enum class TechnicianListMode(
    val screenTitle: String,
    val screenSubtitle: String,
    val emptyMessage: String,
    val includedStatuses: List<String>
) {
    TEAM_UNASSIGNED(
        screenTitle = "Pendientes del equipo",
        screenSubtitle = "Incidencias de tu equipo sin técnico asignado y listas para que las cojas.",
        emptyMessage = "No hay pendientes del equipo ahora mismo.",
        includedStatuses = listOf("OPEN", "IN_PROGRESS", "RESOLVED")
    ),
    MY_ASSIGNED(
        screenTitle = "Asignadas a mí",
        screenSubtitle = "Incidencias que estás gestionando o que siguen vinculadas a ti.",
        emptyMessage = "No tienes incidencias asignadas ahora mismo.",
        includedStatuses = listOf("OPEN", "IN_PROGRESS", "RESOLVED")
    )
}
