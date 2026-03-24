package com.incidencias.data.remote.dto.user

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)