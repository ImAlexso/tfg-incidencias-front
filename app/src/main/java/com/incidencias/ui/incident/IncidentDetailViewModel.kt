package com.incidencias.ui.incident

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.incidencias.data.repository.AttachmentRepository
import com.incidencias.data.repository.CatalogRepository
import com.incidencias.data.repository.IncidentRepository
import com.incidencias.data.repository.MessageRepository
import com.incidencias.session.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class IncidentDetailViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val IN_PROGRESS_STATUS_ID = 2L
    }

    private val appContext = application.applicationContext

    private val incidentRepository = IncidentRepository(appContext)
    private val messageRepository = MessageRepository(appContext)
    private val attachmentRepository = AttachmentRepository(appContext)
    private val catalogRepository = CatalogRepository(appContext)
    private val sessionManager = SessionManager(appContext)

    private val _uiState = MutableStateFlow(IncidentDetailUiState())
    val uiState: StateFlow<IncidentDetailUiState> = _uiState

    private val _events = MutableSharedFlow<IncidentDetailEvent>()
    val events: SharedFlow<IncidentDetailEvent> = _events

    fun initialize(incidentId: Long) {
        if (_uiState.value.incidentId == incidentId && _uiState.value.detail != null) return

        viewModelScope.launch {
            val role = sessionManager.roleFlow.first().orEmpty().uppercase()
            val userId = sessionManager.userIdFlow.first()

            _uiState.value = _uiState.value.copy(
                incidentId = incidentId,
                role = role,
                userId = userId
            )

            loadDetail()
        }
    }

    fun loadDetail() {
        val incidentId = _uiState.value.incidentId
        if (incidentId == -1L) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val response = incidentRepository.getIncidentDetail(incidentId)

                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = response.body(),
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = null,
                        errorMessage = when (response.code()) {
                            401 -> "La sesión ha expirado"
                            403 -> "No tienes permiso para ver esta incidencia"
                            404 -> "La incidencia no existe"
                            else -> "No se pudo cargar el detalle"
                        }
                    )
                }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    detail = null,
                    errorMessage = "No se pudo conectar con el servidor"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    detail = null,
                    errorMessage = e.message ?: "Error al cargar el detalle"
                )
            }
        }
    }

    fun canStartProgress(): Boolean {
        val state = _uiState.value
        val detail = state.detail ?: return false

        return state.role == "TECHNICIAN" &&
                detail.statusName.equals("OPEN", ignoreCase = true) &&
                detail.assignedTechnicianId != null &&
                detail.assignedTechnicianId == state.userId
    }

    fun assignToMe() {
        val state = _uiState.value
        val incidentId = state.incidentId
        val userId = state.userId ?: run {
            emitMessage("No se pudo identificar al usuario")
            return
        }

        val detail = state.detail ?: run {
            emitMessage("No hay detalle cargado")
            return
        }

        if (!detail.canAssignToMe) {
            emitMessage("No puedes asignarte esta incidencia")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isActionLoading = true)

            try {
                val response = incidentRepository.assignTechnician(incidentId, userId)
                _uiState.value = _uiState.value.copy(isActionLoading = false)

                if (response.isSuccessful) {
                    _events.emit(IncidentDetailEvent.ShowMessage("Incidencia asignada"))
                    loadDetail()
                } else {
                    _events.emit(
                        IncidentDetailEvent.ShowMessage(
                            when (response.code()) {
                                400, 409 -> "No se puede asignar la incidencia en su estado actual"
                                403 -> "No tienes permiso para asignarte esta incidencia"
                                404 -> "La incidencia no existe"
                                else -> "No se pudo asignar"
                            }
                        )
                    )
                }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(isActionLoading = false)
                _events.emit(IncidentDetailEvent.ShowMessage("No se pudo conectar con el servidor"))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isActionLoading = false)
                _events.emit(IncidentDetailEvent.ShowMessage(e.message ?: "Error al asignar"))
            }
        }
    }

    fun startProgress() {
        val state = _uiState.value
        val incidentId = state.incidentId
        val detail = state.detail ?: run {
            emitMessage("No hay detalle cargado")
            return
        }

        if (!canStartProgress()) {
            emitMessage("No puedes iniciar esta incidencia")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true)

            try {
                val response = incidentRepository.updateIncidentStatus(
                    id = incidentId,
                    statusId = IN_PROGRESS_STATUS_ID
                )

                _uiState.value = _uiState.value.copy(isActionLoading = false)

                if (response.isSuccessful) {
                    _events.emit(IncidentDetailEvent.ShowMessage("Incidencia en progreso"))
                    loadDetail()
                } else {
                    _events.emit(
                        IncidentDetailEvent.ShowMessage(
                            when (response.code()) {
                                400, 409 -> "No se puede iniciar la incidencia en su estado actual"
                                403 -> "No tienes permiso para iniciar esta incidencia"
                                404 -> "La incidencia no existe"
                                else -> "No se pudo iniciar la incidencia"
                            }
                        )
                    )
                }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(isActionLoading = false)
                _events.emit(IncidentDetailEvent.ShowMessage("No se pudo conectar con el servidor"))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isActionLoading = false)
                _events.emit(IncidentDetailEvent.ShowMessage(e.message ?: "Error al iniciar la incidencia"))
            }
        }
    }

    fun resolveIncident() {
        val state = _uiState.value
        val incidentId = state.incidentId
        val detail = state.detail ?: run {
            emitMessage("No hay detalle cargado")
            return
        }

        if (!detail.canResolve) {
            emitMessage("No puedes resolver esta incidencia")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true)

            try {
                val response = incidentRepository.resolveIncident(incidentId)
                _uiState.value = _uiState.value.copy(isActionLoading = false)

                if (response.isSuccessful) {
                    _events.emit(IncidentDetailEvent.ShowMessage("Incidencia resuelta"))
                    loadDetail()
                } else {
                    _events.emit(
                        IncidentDetailEvent.ShowMessage(
                            when (response.code()) {
                                400, 409 -> "No se puede resolver la incidencia en su estado actual"
                                403 -> "No tienes permiso para resolver esta incidencia"
                                404 -> "La incidencia no existe"
                                else -> "No se pudo resolver"
                            }
                        )
                    )
                }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(isActionLoading = false)
                _events.emit(IncidentDetailEvent.ShowMessage("No se pudo conectar con el servidor"))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isActionLoading = false)
                _events.emit(IncidentDetailEvent.ShowMessage(e.message ?: "Error al resolver"))
            }
        }
    }

    fun closeIncident() {
        val state = _uiState.value
        val incidentId = state.incidentId
        val detail = state.detail ?: run {
            emitMessage("No hay detalle cargado")
            return
        }

        if (!detail.canClose) {
            emitMessage("No puedes cerrar esta incidencia")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true)

            try {
                val response = incidentRepository.closeIncident(incidentId)
                _uiState.value = _uiState.value.copy(isActionLoading = false)

                if (response.isSuccessful) {
                    _events.emit(IncidentDetailEvent.ShowMessage("Incidencia cerrada"))
                    loadDetail()
                } else {
                    _events.emit(
                        IncidentDetailEvent.ShowMessage(
                            when (response.code()) {
                                400, 409 -> "No se puede cerrar la incidencia en su estado actual"
                                403 -> "No tienes permiso para cerrar esta incidencia"
                                404 -> "La incidencia no existe"
                                else -> "No se pudo cerrar"
                            }
                        )
                    )
                }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(isActionLoading = false)
                _events.emit(IncidentDetailEvent.ShowMessage("No se pudo conectar con el servidor"))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isActionLoading = false)
                _events.emit(IncidentDetailEvent.ShowMessage(e.message ?: "Error al cerrar"))
            }
        }
    }

    fun requestPriorityOptions() {
        val detail = _uiState.value.detail ?: run {
            emitMessage("No hay detalle cargado")
            return
        }

        if (!detail.canChangePriority) {
            emitMessage("No puedes cambiar la prioridad de esta incidencia")
            return
        }

        viewModelScope.launch {
            try {
                val response = catalogRepository.getPriorities()
                if (response.isSuccessful) {
                    val priorities = response.body().orEmpty().filter { it.active }
                    _events.emit(IncidentDetailEvent.ShowPriorityPicker(priorities))
                } else {
                    _events.emit(IncidentDetailEvent.ShowMessage("No se pudieron cargar las prioridades"))
                }
            } catch (e: IOException) {
                _events.emit(IncidentDetailEvent.ShowMessage("No se pudo conectar con el servidor"))
            } catch (e: Exception) {
                _events.emit(IncidentDetailEvent.ShowMessage(e.message ?: "Error cargando prioridades"))
            }
        }
    }

    fun updatePriority(priorityId: Long) {
        val state = _uiState.value
        val incidentId = state.incidentId
        val detail = state.detail ?: run {
            emitMessage("No hay detalle cargado")
            return
        }

        if (!detail.canChangePriority) {
            emitMessage("No puedes cambiar la prioridad de esta incidencia")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true)

            try {
                val response = incidentRepository.updateIncidentPriority(incidentId, priorityId)
                _uiState.value = _uiState.value.copy(isActionLoading = false)

                if (response.isSuccessful) {
                    _events.emit(IncidentDetailEvent.ShowMessage("Prioridad actualizada"))
                    loadDetail()
                } else {
                    _events.emit(
                        IncidentDetailEvent.ShowMessage(
                            when (response.code()) {
                                400, 409 -> "No se puede cambiar la prioridad en el estado actual"
                                403 -> "No tienes permiso para cambiar la prioridad"
                                404 -> "La incidencia no existe"
                                else -> "No se pudo actualizar la prioridad"
                            }
                        )
                    )
                }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(isActionLoading = false)
                _events.emit(IncidentDetailEvent.ShowMessage("No se pudo conectar con el servidor"))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isActionLoading = false)
                _events.emit(IncidentDetailEvent.ShowMessage(e.message ?: "Error actualizando prioridad"))
            }
        }
    }

    fun requestTeamOptions() {
        val detail = _uiState.value.detail ?: run {
            emitMessage("No hay detalle cargado")
            return
        }

        if (!detail.canChangeTeam) {
            emitMessage("No puedes cambiar el equipo de esta incidencia")
            return
        }

        viewModelScope.launch {
            try {
                val response = catalogRepository.getTeams()
                if (response.isSuccessful) {
                    val teams = response.body().orEmpty().filter { it.active }
                    _events.emit(IncidentDetailEvent.ShowTeamPicker(teams))
                } else {
                    _events.emit(IncidentDetailEvent.ShowMessage("No se pudieron cargar los equipos"))
                }
            } catch (e: IOException) {
                _events.emit(IncidentDetailEvent.ShowMessage("No se pudo conectar con el servidor"))
            } catch (e: Exception) {
                _events.emit(IncidentDetailEvent.ShowMessage(e.message ?: "Error cargando equipos"))
            }
        }
    }

    fun updateTeam(teamId: Long) {
        val state = _uiState.value
        val incidentId = state.incidentId
        val detail = state.detail ?: run {
            emitMessage("No hay detalle cargado")
            return
        }

        if (!detail.canChangeTeam) {
            emitMessage("No puedes cambiar el equipo de esta incidencia")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true)

            try {
                val response = incidentRepository.updateIncidentTeam(incidentId, teamId)
                _uiState.value = _uiState.value.copy(isActionLoading = false)

                if (response.isSuccessful) {
                    _events.emit(IncidentDetailEvent.ShowMessage("Equipo actualizado"))
                    loadDetail()
                } else {
                    _events.emit(
                        IncidentDetailEvent.ShowMessage(
                            when (response.code()) {
                                400, 409 -> "No se puede cambiar el equipo en el estado actual"
                                403 -> "No tienes permiso para cambiar el equipo"
                                404 -> "La incidencia no existe"
                                else -> "No se pudo actualizar el equipo"
                            }
                        )
                    )
                }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(isActionLoading = false)
                _events.emit(IncidentDetailEvent.ShowMessage("No se pudo conectar con el servidor"))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isActionLoading = false)
                _events.emit(IncidentDetailEvent.ShowMessage(e.message ?: "Error actualizando equipo"))
            }
        }
    }

    fun requestAssignableTechnicians() {
        val detail = _uiState.value.detail ?: run {
            emitMessage("No hay detalle cargado")
            return
        }

        if (!detail.canAssignTechnician) {
            emitMessage("No puedes asignar técnico en esta incidencia")
            return
        }

        viewModelScope.launch {
            try {
                val response = incidentRepository.getAssignableTechnicians(_uiState.value.incidentId)
                if (response.isSuccessful) {
                    _events.emit(IncidentDetailEvent.ShowTechnicianPicker(response.body().orEmpty()))
                } else {
                    _events.emit(IncidentDetailEvent.ShowMessage("No se pudieron cargar los técnicos"))
                }
            } catch (e: IOException) {
                _events.emit(IncidentDetailEvent.ShowMessage("No se pudo conectar con el servidor"))
            } catch (e: Exception) {
                _events.emit(IncidentDetailEvent.ShowMessage(e.message ?: "Error cargando técnicos"))
            }
        }
    }

    fun assignTechnician(technicianId: Long) {
        val state = _uiState.value
        val incidentId = state.incidentId
        val detail = state.detail ?: run {
            emitMessage("No hay detalle cargado")
            return
        }

        if (!detail.canAssignTechnician) {
            emitMessage("No puedes asignar técnico en esta incidencia")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true)

            try {
                val response = incidentRepository.assignTechnician(incidentId, technicianId)
                _uiState.value = _uiState.value.copy(isActionLoading = false)

                if (response.isSuccessful) {
                    _events.emit(IncidentDetailEvent.ShowMessage("Técnico asignado"))
                    loadDetail()
                } else {
                    _events.emit(
                        IncidentDetailEvent.ShowMessage(
                            when (response.code()) {
                                400, 409 -> "No se puede asignar el técnico en el estado actual"
                                403 -> "No tienes permiso para asignar técnico"
                                404 -> "La incidencia no existe"
                                else -> "No se pudo asignar el técnico"
                            }
                        )
                    )
                }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(isActionLoading = false)
                _events.emit(IncidentDetailEvent.ShowMessage("No se pudo conectar con el servidor"))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isActionLoading = false)
                _events.emit(IncidentDetailEvent.ShowMessage(e.message ?: "Error asignando técnico"))
            }
        }
    }

    fun sendPublicMessage(message: String) {
        sendMessage(message = message, internal = false)
    }

    fun sendInternalMessage(message: String) {
        sendMessage(message = message, internal = true)
    }

    private fun sendMessage(message: String, internal: Boolean) {
        val incidentId = _uiState.value.incidentId
        if (incidentId == -1L) return

        val detail = _uiState.value.detail ?: run {
            emitMessage("No hay detalle cargado")
            return
        }

        if (detail.statusName.equals("CLOSED", ignoreCase = true)) {
            emitMessage("No se pueden enviar mensajes en una incidencia cerrada")
            return
        }

        if (message.isBlank()) {
            emitMessage("El mensaje no puede estar vacío")
            return
        }

        viewModelScope.launch {
            _uiState.value = if (internal) {
                _uiState.value.copy(isSendingInternalMessage = true)
            } else {
                _uiState.value.copy(isSendingPublicMessage = true)
            }

            try {
                val response = if (internal) {
                    messageRepository.addInternalMessage(incidentId, message)
                } else {
                    messageRepository.addPublicMessage(incidentId, message)
                }

                _uiState.value = if (internal) {
                    _uiState.value.copy(isSendingInternalMessage = false)
                } else {
                    _uiState.value.copy(isSendingPublicMessage = false)
                }

                if (response.isSuccessful) {
                    _events.emit(
                        IncidentDetailEvent.ShowMessage(
                            if (internal) "Mensaje interno enviado" else "Mensaje enviado"
                        )
                    )
                    loadDetail()
                } else {
                    _events.emit(
                        IncidentDetailEvent.ShowMessage(
                            if (internal) "No se pudo enviar el mensaje interno" else "No se pudo enviar el mensaje"
                        )
                    )
                }
            } catch (e: IOException) {
                _uiState.value = if (internal) {
                    _uiState.value.copy(isSendingInternalMessage = false)
                } else {
                    _uiState.value.copy(isSendingPublicMessage = false)
                }
                _events.emit(IncidentDetailEvent.ShowMessage("No se pudo conectar con el servidor"))
            } catch (e: Exception) {
                _uiState.value = if (internal) {
                    _uiState.value.copy(isSendingInternalMessage = false)
                } else {
                    _uiState.value.copy(isSendingPublicMessage = false)
                }

                _events.emit(
                    IncidentDetailEvent.ShowMessage(
                        e.message ?: if (internal) "Error al enviar mensaje interno" else "Error al enviar mensaje"
                    )
                )
            }
        }
    }

    fun uploadAttachment(uri: Uri, fileName: String?) {
        val incidentId = _uiState.value.incidentId
        if (incidentId == -1L) return

        val detail = _uiState.value.detail ?: run {
            emitMessage("No hay detalle cargado")
            return
        }

        if (detail.statusName.equals("CLOSED", ignoreCase = true)) {
            emitMessage("No se pueden adjuntar archivos en una incidencia cerrada")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingAttachment = true)

            try {
                val safeFileName = fileName?.takeIf { it.isNotBlank() } ?: "archivo"
                val tempFile = copyUriToTempFile(uri, safeFileName)
                val mimeType = appContext.contentResolver.getType(uri) ?: "application/octet-stream"

                val requestFile = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                val multipart = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)

                val role = _uiState.value.role
                val attachmentType = when (role) {
                    "TECHNICIAN" -> "TECHNICIAN_EVIDENCE"
                    "MANAGER", "ADMIN" -> "MANAGER_EVIDENCE"
                    else -> "USER_EVIDENCE"
                }

                val typeBody = attachmentType.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = attachmentRepository.uploadAttachment(incidentId, multipart, typeBody)

                _uiState.value = _uiState.value.copy(isUploadingAttachment = false)

                if (response.isSuccessful) {
                    _events.emit(IncidentDetailEvent.ShowMessage("Adjunto subido"))
                    loadDetail()
                } else {
                    _events.emit(IncidentDetailEvent.ShowMessage("No se pudo subir el archivo"))
                }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(isUploadingAttachment = false)
                _events.emit(IncidentDetailEvent.ShowMessage("No se pudo conectar con el servidor"))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isUploadingAttachment = false)
                _events.emit(IncidentDetailEvent.ShowMessage(e.message ?: "Error al subir archivo"))
            }
        }
    }

    fun openAttachment(attachmentId: Long) {
        val incidentId = _uiState.value.incidentId
        if (incidentId == -1L) return

        viewModelScope.launch {
            try {
                val response = attachmentRepository.getDownloadUrl(incidentId, attachmentId)

                if (response.isSuccessful && response.body() != null) {
                    _events.emit(IncidentDetailEvent.OpenDownloadUrl(response.body()!!.url))
                } else {
                    _events.emit(IncidentDetailEvent.ShowMessage("No se pudo obtener la URL del adjunto"))
                }
            } catch (e: IOException) {
                _events.emit(IncidentDetailEvent.ShowMessage("No se pudo conectar con el servidor"))
            } catch (e: Exception) {
                _events.emit(IncidentDetailEvent.ShowMessage(e.message ?: "Error al abrir adjunto"))
            }
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _events.emit(IncidentDetailEvent.ShowMessage(message))
        }
    }

    private fun copyUriToTempFile(uri: Uri, fileName: String): File {
        val inputStream = appContext.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("No se pudo abrir el archivo")

        val tempFile = File(appContext.cacheDir, fileName)

        FileOutputStream(tempFile).use { output ->
            inputStream.copyTo(output)
        }

        inputStream.close()
        return tempFile
    }
}