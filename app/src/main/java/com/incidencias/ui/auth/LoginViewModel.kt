package com.incidencias.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.incidencias.data.repository.AuthRepository
import com.incidencias.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
                    _uiState.value = LoginUiState.Error("Credenciales incorrectas o error de autenticación")
                    return@launch
                }

                val token = loginResponse.body()!!.token
                sessionManager.saveToken(token)

                val profileResponse = authRepository.getMyProfile()

                if (!profileResponse.isSuccessful || profileResponse.body() == null) {
                    sessionManager.clearSession()
                    _uiState.value = LoginUiState.Error("No se pudo recuperar el perfil del usuario")
                    return@launch
                }

                val user = profileResponse.body()!!

                sessionManager.saveUserData(
                    userId = user.id,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    email = user.email,
                    role = user.role,
                    teamId = user.teamId,
                    teamName = user.teamName
                )

                _uiState.value = LoginUiState.Success(user.role)

            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(
                    e.message ?: "Error inesperado durante el login"
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}