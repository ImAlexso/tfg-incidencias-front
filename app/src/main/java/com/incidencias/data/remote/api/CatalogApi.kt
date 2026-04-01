package com.incidencias.data.remote.api

import com.incidencias.data.remote.dto.catalog.CategoryResponse
import com.incidencias.data.remote.dto.catalog.PriorityResponse
import com.incidencias.data.remote.dto.catalog.TeamResponse
import com.incidencias.data.remote.dto.catalog.TeamTechnicianResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface CatalogApi {

    @GET("catalogs/categories")
    suspend fun getCategories(): Response<List<CategoryResponse>>

    @GET("catalogs/priorities")
    suspend fun getPriorities(): Response<List<PriorityResponse>>

    @GET("catalogs/teams")
    suspend fun getTeams(): Response<List<TeamResponse>>

    @GET("catalogs/teams/{teamId}/technicians")
    suspend fun getTechniciansByTeam(
        @Path("teamId") teamId: Long
    ): Response<List<TeamTechnicianResponse>>
}