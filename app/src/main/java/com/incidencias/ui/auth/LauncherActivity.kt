package com.incidencias.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.incidencias.session.SessionManager
import com.incidencias.ui.admin.AdminMainActivity
import com.incidencias.ui.manager.ManagerMainActivity
import com.incidencias.ui.technician.TechnicianMainActivity
import com.incidencias.ui.user.UserMainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LauncherActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(applicationContext)

        lifecycleScope.launch {
            applySavedTheme()

            val token = sessionManager.tokenFlow.first()
            val role = sessionManager.roleFlow.first()

            if (!token.isNullOrBlank() && !role.isNullOrBlank()) {
                navigateByRole(role)
            } else {
                startActivity(Intent(this@LauncherActivity, LoginActivity::class.java))
                finish()
            }
        }
    }

    private suspend fun applySavedTheme() {
        val darkMode = sessionManager.darkModeFlow.first()
        AppCompatDelegate.setDefaultNightMode(
            if (darkMode) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        )
    }

    private fun navigateByRole(role: String) {
        val intent = when (role.uppercase()) {
            "USER" -> Intent(this, UserMainActivity::class.java)
            "TECHNICIAN" -> Intent(this, TechnicianMainActivity::class.java)
            "MANAGER" -> Intent(this, ManagerMainActivity::class.java)
            "ADMIN" -> Intent(this, AdminMainActivity::class.java)
            else -> Intent(this, LoginActivity::class.java)
        }

        startActivity(intent)
        finish()
    }
}