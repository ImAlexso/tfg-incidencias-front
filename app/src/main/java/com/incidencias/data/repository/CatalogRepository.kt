package com.incidencias.data.repository

import android.content.Context
import com.incidencias.data.remote.api.CatalogApi
import com.incidencias.data.remote.dto.catalog.CategoryResponse
import com.incidencias.data.remote.dto.catalog.PriorityResponse
import com.incidencias.data.remote.dto.catalog.TeamResponse
import com.incidencias.data.remote.dto.catalog.TeamTechnicianResponse
import com.incidencias.data.remote.retrofit.RetrofitClient
import retrofit2.Response

class CatalogRepository(context: Context) {

    private val catalogApi: CatalogApi =
        RetrofitClient.createService(context, CatalogApi::class.java)

    suspend fun getCategories(): Response<List<CategoryResponse>> {
        return catalogApi.getCategories()
    }

    suspend fun getPriorities(): Response<List<PriorityResponse>> {
        return catalogApi.getPriorities()
    }

    suspend fun getTeams(): Response<List<TeamResponse>> {
        return catalogApi.getTeams()
    }

    suspend fun getTechniciansByTeam(teamId: Long): Response<List<TeamTechnicianResponse>> {
        return catalogApi.getTechniciansByTeam(teamId)
    }
}