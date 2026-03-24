package com.incidencias.ui.settings

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.incidencias.data.repository.UserRepository
import com.incidencias.databinding.ActivityChangePasswordBinding
import kotlinx.coroutines.launch

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userRepository = UserRepository(applicationContext)

        binding.btnSavePassword.setOnClickListener {
            changePassword()
        }
    }

    private fun changePassword() {
        val currentPassword = binding.etCurrentPassword.text?.toString()?.trim().orEmpty()
        val newPassword = binding.etNewPassword.text?.toString()?.trim().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString()?.trim().orEmpty()

        if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "La nueva contraseña no coincide", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            binding.progressBarChangePassword.visibility = android.view.View.VISIBLE
            binding.btnSavePassword.isEnabled = false

            try {
                val response = userRepository.changePassword(currentPassword, newPassword)

                binding.progressBarChangePassword.visibility = android.view.View.GONE
                binding.btnSavePassword.isEnabled = true

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ChangePasswordActivity,
                        "Contraseña actualizada",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@ChangePasswordActivity,
                        "No se pudo cambiar la contraseña",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                binding.progressBarChangePassword.visibility = android.view.View.GONE
                binding.btnSavePassword.isEnabled = true
                Toast.makeText(
                    this@ChangePasswordActivity,
                    e.message ?: "Error al cambiar contraseña",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}