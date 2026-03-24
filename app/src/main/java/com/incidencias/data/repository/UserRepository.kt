package com.incidencias.data.repository

import android.content.Context
import com.incidencias.data.remote.api.UserApi
import com.incidencias.data.remote.dto.user.ChangePasswordRequest
import com.incidencias.data.remote.dto.user.UpdateUserProfileRequest
import com.incidencias.data.remote.dto.user.UserMeResponse
import com.incidencias.data.remote.retrofit.RetrofitClient
import retrofit2.Response

class UserRepository(context: Context) {

    private val userApi: UserApi =
        RetrofitClient.createService(context, UserApi::class.java)

    suspend fun getMyProfile(): Response<UserMeResponse> {
        return userApi.getMyProfile()
    }

    suspend fun updateProfile(firstName: String, lastName: String): Response<Unit> {
        return userApi.updateProfile(UpdateUserProfileRequest(firstName, lastName))
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Response<Unit> {
        return userApi.changePassword(ChangePasswordRequest(currentPassword, newPassword))
    }
}