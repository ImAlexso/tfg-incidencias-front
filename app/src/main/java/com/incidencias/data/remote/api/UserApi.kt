package com.incidencias.data.remote.api

import com.incidencias.data.remote.dto.user.ChangePasswordRequest
import com.incidencias.data.remote.dto.user.UpdateUserProfileRequest
import com.incidencias.data.remote.dto.user.UserMeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface UserApi {

    @GET("users/me")
    suspend fun getMyProfile(): Response<UserMeResponse>

    @PATCH("users/me")
    suspend fun updateProfile(
        @Body request: UpdateUserProfileRequest
    ): Response<Unit>

    @PATCH("users/me/password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest
    ): Response<Unit>
}