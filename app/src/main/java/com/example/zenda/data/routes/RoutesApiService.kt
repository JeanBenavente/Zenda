package com.example.zenda.data.routes

import retrofit2.http.Body
import retrofit2.http.POST

interface RoutesApiService {

    @POST("directions/v2:computeRoutes")
    suspend fun computeRoutes(
        @Body body: ComputeRoutesRequestDto
    ): ComputeRoutesResponseDto
}

