package com.incidencias.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.incidencias.data.repository.UserRepository
import com.incidencias.databinding.ActivityEditProfileBinding
import com.incidencias.session.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(applicationContext)
        userRepository = UserRepository(applicationContext)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadCurrentData()

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadCurrentData() {
        lifecycleScope.launch {
            binding.etFirstName.setText(sessionManager.firstNameFlow.first().orEmpty())
            binding.etLastName.setText(sessionManager.lastNameFlow.first().orEmpty())
        }
    }

    private fun saveProfile() {
        val firstName = binding.etFirstName.text?.toString()?.trim().orEmpty()
        val lastName = binding.etLastName.text?.toString()?.trim().orEmpty()

        if (firstName.isBlank() || lastName.isBlank()) {
            Toast.makeText(this, "Nombre y apellido son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            binding.progressBarEditProfile.visibility = View.VISIBLE
            binding.btnSaveProfile.isEnabled = false

            try {
                val response = userRepository.updateProfile(firstName, lastName)

                binding.progressBarEditProfile.visibility = View.GONE
                binding.btnSaveProfile.isEnabled = true

                if (response.isSuccessful) {
                    val userId = sessionManager.userIdFlow.first() ?: 0L
                    val email = sessionManager.emailFlow.first().orEmpty()
                    val role = sessionManager.roleFlow.first().orEmpty()
                    val teamId = sessionManager.teamIdFlow.first()
                    val teamName = sessionManager.teamNameFlow.first()

                    sessionManager.saveUserData(
                        userId = userId,
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        role = role,
                        teamId = teamId,
                        teamName = teamName
                    )

                    Toast.makeText(this@EditProfileActivity, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@EditProfileActivity, "No se pudo actualizar el perfil", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.progressBarEditProfile.visibility = View.GONE
                binding.btnSaveProfile.isEnabled = true
                Toast.makeText(
                    this@EditProfileActivity,
                    e.message ?: "Error al actualizar perfil",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}