package com.example.inventariodaws.services

import retrofit2.http.Body
import retrofit2.http.POST
import com.example.inventariodaws.models.LoginData
import com.example.inventariodaws.models.LoginDataResponse
import retrofit2.Response

interface ApiService {

    @POST("/api/Login")
    suspend fun loginApp(@Body dataLogin: LoginData): Response<LoginDataResponse>

}