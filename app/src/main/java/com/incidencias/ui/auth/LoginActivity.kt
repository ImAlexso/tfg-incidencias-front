package com.incidencias.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.incidencias.databinding.ActivityLoginBinding
import com.incidencias.session.SessionManager
import com.incidencias.ui.manager.ManagerMainActivity
import com.incidencias.ui.technician.TechnicianMainActivity
import com.incidencias.ui.user.UserMainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(applicationContext)

        applySavedTheme()
        setupListeners()
        observeUiState()
    }

    private fun applySavedTheme() {
        lifecycleScope.launch {
            val darkMode = sessionManager.darkModeFlow.first()
            AppCompatDelegate.setDefaultNightMode(
                if (darkMode) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
            )
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            submitLogin()
        }

        binding.etPassword.setOnEditorActionListener { _, actionId, event ->
            val isDoneAction = actionId == EditorInfo.IME_ACTION_DONE
            val isEnterKey =
                event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN

            if (isDoneAction || isEnterKey) {
                submitLogin()
                true
            } else {
                false
            }
        }

        binding.etEmail.doAfterTextChanged {
            clearInputErrors()
            hideInlineError()
        }

        binding.etPassword.doAfterTextChanged {
            clearInputErrors()
            hideInlineError()
        }
    }

    private fun submitLogin() {
        hideInlineError()

        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString()?.trim().orEmpty()

        if (email.isBlank()) {
            binding.tilEmail.error = "Introduce tu correo"
        }

        if (password.isBlank()) {
            binding.tilPassword.error = "Introduce tu contraseña"
        }

        if (email.isBlank() || password.isBlank()) {
            return
        }

        viewModel.login(email, password)
    }

    private fun clearInputErrors() {
        binding.tilEmail.error = null
        binding.tilPassword.error = null
    }

    private fun showInlineError(message: String) {
        binding.tvLoginError.text = message
        binding.tvLoginError.visibility = View.VISIBLE
    }

    private fun hideInlineError() {
        binding.tvLoginError.visibility = View.GONE
        binding.tvLoginError.text = ""
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.tilEmail.isEnabled = !isLoading
        binding.tilPassword.isEnabled = !isLoading
        binding.btnLogin.text = if (isLoading) "Accediendo..." else "Entrar"
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is LoginUiState.Idle -> {
                            setLoading(false)
                        }

                        is LoginUiState.Loading -> {
                            hideInlineError()
                            setLoading(true)
                        }

                        is LoginUiState.Success -> {
                            setLoading(false)
                            navigateByRole(state.role)
                        }

                        is LoginUiState.Error -> {
                            setLoading(false)
                            showInlineError(state.message)
                            viewModel.resetState()
                        }
                    }
                }
            }
        }
    }

    private fun navigateByRole(role: String) {
        val intent = when (role.uppercase()) {
            "USER" -> Intent(this, UserMainActivity::class.java)
            "TECHNICIAN" -> Intent(this, TechnicianMainActivity::class.java)
            "MANAGER" -> Intent(this, ManagerMainActivity::class.java)
            else -> {
                showInlineError("Rol no reconocido: $role")
                return
            }
        }

        startActivity(intent)
        finish()
    }
}