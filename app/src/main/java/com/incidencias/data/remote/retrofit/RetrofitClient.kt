package com.incidencias.data.remote.retrofit

import android.content.Context
import com.incidencias.data.remote.interceptor.AuthInterceptor
import com.incidencias.session.SessionManager
import com.incidencias.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private fun buildRetrofit(context: Context): Retrofit {
        val sessionManager = SessionManager(context)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = AuthInterceptor(sessionManager)

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> createService(context: Context, serviceClass: Class<T>): T {
        return buildRetrofit(context).create(serviceClass)
    }
}