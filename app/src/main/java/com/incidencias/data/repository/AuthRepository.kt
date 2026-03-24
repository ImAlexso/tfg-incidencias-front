package com.incidencias.data.repository

import android.content.Context
import com.incidencias.data.remote.api.AuthApi
import com.incidencias.data.remote.api.UserApi
import com.incidencias.data.remote.dto.auth.LoginRequest
import com.incidencias.data.remote.dto.auth.LoginResponse
import com.incidencias.data.remote.dto.user.UserMeResponse
import com.incidencias.data.remote.retrofit.RetrofitClient
import retrofit2.Response

class AuthRepository(context: Context) {

    private val authApi: AuthApi =
        RetrofitClient.createService(context, AuthApi::class.java)

    private val userApi: UserApi =
        RetrofitClient.createService(context, UserApi::class.java)

    suspend fun login(email: String, password: String): Response<LoginResponse> {
        return authApi.login(LoginRequest(email, password))
    }

    suspend fun getMyProfile(): Response<UserMeResponse> {
        return userApi.getMyProfile()
    }
}