package com.incidencias.ui.user.incidents

enum class IncidentListMode(
    val screenTitle: String,
    val screenSubtitle: String,
    val emptyMessage: String
) {
    ACTIVE(
        screenTitle = "Incidencias activas",
        screenSubtitle = "Abiertas, en curso o pendientes de cierre.",
        emptyMessage = "No tienes incidencias activas."
    ),
    HISTORY(
        screenTitle = "Histórico",
        screenSubtitle = "Incidencias cerradas anteriormente.",
        emptyMessage = "No tienes incidencias en el histórico."
    )
}