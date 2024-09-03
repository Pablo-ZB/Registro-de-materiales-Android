package com.example.inventariodaws.models

import com.google.gson.annotations.SerializedName

data class LoginDataResponse(
    @SerializedName("user") val user: String,
    @SerializedName("acesso") val acesso: String
)
