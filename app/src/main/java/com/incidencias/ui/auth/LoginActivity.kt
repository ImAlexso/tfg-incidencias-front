package com.incidencias.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.incidencias.databinding.ActivityLoginBinding
import com.incidencias.ui.admin.AdminMainActivity
import com.incidencias.ui.manager.ManagerMainActivity
import com.incidencias.ui.technician.TechnicianMainActivity
import com.incidencias.ui.user.UserMainActivity
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.incidencias.session.SessionManager
import kotlinx.coroutines.flow.first

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
        checkExistingSession()
        setupListeners()
        observeUiState()
    }
    private fun applySavedTheme() {
        lifecycleScope.launch {
            val darkMode = sessionManager.darkModeFlow.first()
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                if (darkMode) {
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                }
            )
        }
    }
    private fun checkExistingSession() {
        lifecycleScope.launch {
            val token = sessionManager.tokenFlow.first()
            val role = sessionManager.roleFlow.first()

            if (!token.isNullOrBlank() && !role.isNullOrBlank()) {
                navigateByRole(role)
            }
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString()?.trim().orEmpty()

            viewModel.login(email, password)
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is LoginUiState.Idle -> {
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.btnLogin.isEnabled = true
                    }

                    is LoginUiState.Loading -> {
                        binding.progressBar.visibility = android.view.View.VISIBLE
                        binding.btnLogin.isEnabled = false
                    }

                    is LoginUiState.Success -> {
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.btnLogin.isEnabled = true
                        navigateByRole(state.role)
                    }

                    is LoginUiState.Error -> {
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.btnLogin.isEnabled = true
                        Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
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
            "ADMIN" -> Intent(this, AdminMainActivity::class.java)
            else -> {
                Toast.makeText(this, "Rol no reconocido: $role", Toast.LENGTH_LONG).show()
                return
            }
        }

        startActivity(intent)
        finish()
    }
}