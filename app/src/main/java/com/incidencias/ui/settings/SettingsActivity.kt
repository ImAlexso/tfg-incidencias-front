package com.incidencias.ui.settings

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.incidencias.R
import com.incidencias.databinding.ActivitySettingsBinding
import com.incidencias.session.SessionManager
import com.incidencias.ui.auth.LoginActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sessionManager: SessionManager

    private var isDarkModeEnabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(applicationContext)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadUserData()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            val firstName = sessionManager.firstNameFlow.first().orEmpty()
            val lastName = sessionManager.lastNameFlow.first().orEmpty()
            val email = sessionManager.emailFlow.first().orEmpty()

            binding.tvFullName.text = "$firstName $lastName".trim()
            binding.tvEmail.text = email

            isDarkModeEnabled = isCurrentlyDarkMode()
            updateThemeIcon(isDarkModeEnabled)
        }
    }

    private fun setupListeners() {
        binding.ivThemeToggle.setOnClickListener {
            val newDarkModeValue = !isCurrentlyDarkMode()

            lifecycleScope.launch {
                sessionManager.saveDarkMode(newDarkModeValue)

                AppCompatDelegate.setDefaultNightMode(
                    if (newDarkModeValue) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }

        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        binding.btnChangePassword.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                sessionManager.clearSession()

                val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    private fun isCurrentlyDarkMode(): Boolean {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    private fun updateThemeIcon(isDarkMode: Boolean) {
        binding.ivThemeToggle.setImageResource(
            if (isDarkMode) R.drawable.ic_light_mode else R.drawable.ic_dark_mode
        )
    }
}