package com.example.inventariodaws

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {
    @POST("api/data")
    fun saveData(@Body dataItem: DataItem): Call<ApiResponse>

    @POST("api/Login/Authenticate")
    fun login(@Body loginRequest: LoginRequest): Call<LoginResponse>
}

data class LoginRequest(val noEmpleado: String)
data class LoginResponse(val message: String)


