package com.incidencias.data.remote.api

import com.incidencias.data.remote.dto.auth.LoginRequest
import com.incidencias.data.remote.dto.auth.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>
}