package com.incidencias.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.incidencias.data.repository.AuthRepository
import com.incidencias.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application.applicationContext)
    private val sessionManager = SessionManager(application.applicationContext)

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Email y contraseña son obligatorios")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            try {
                val loginResponse = authRepository.login(email, password)

                if (!loginResponse.isSuccessful || loginResponse.body()?.token.isNullOrBlank()) {
                    _uiState.value = when (loginResponse.code()) {
                        401 -> LoginUiState.Error("Credenciales incorrectas")
                        403 -> LoginUiState.Error("No tienes permiso para acceder")
                        else -> LoginUiState.Error("No se pudo iniciar sesión")
                    }
                    return@launch
                }

                val token = loginResponse.body()!!.token
                sessionManager.saveToken(token)

                loadProfileAndFinishLogin(sessionExpiredMessage = "La sesión ha expirado")
            } catch (e: IOException) {
                _uiState.value = LoginUiState.Error("No se pudo conectar con el servidor")
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "Error inesperado al iniciar sesión")
            }
        }
    }

    fun validateExistingSession() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            loadProfileAndFinishLogin(sessionExpiredMessage = "La sesión ha expirado. Vuelve a iniciar sesión")
        }
    }

    private suspend fun loadProfileAndFinishLogin(sessionExpiredMessage: String) {
        try {
            val profileResponse = authRepository.getMyProfile()

            if (!profileResponse.isSuccessful || profileResponse.body() == null) {
                sessionManager.clearSession()

                _uiState.value = when (profileResponse.code()) {
                    401 -> LoginUiState.Error(sessionExpiredMessage)
                    403 -> LoginUiState.Error("No tienes permiso para acceder")
                    else -> LoginUiState.Error("No se pudo recuperar el perfil del usuario")
                }
                return
            }

            val profile = profileResponse.body()!!

            sessionManager.saveUserData(
                userId = profile.id,
                firstName = profile.firstName,
                lastName = profile.lastName,
                email = profile.email,
                role = profile.role,
                teamId = profile.teamId,
                teamName = profile.teamName
            )

            _uiState.value = LoginUiState.Success(profile.role)
        } catch (e: IOException) {
            sessionManager.clearSession()
            _uiState.value = LoginUiState.Error("No se pudo conectar con el servidor")
        } catch (e: Exception) {
            sessionManager.clearSession()
            _uiState.value = LoginUiState.Error(e.message ?: "Error al validar la sesión")
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}