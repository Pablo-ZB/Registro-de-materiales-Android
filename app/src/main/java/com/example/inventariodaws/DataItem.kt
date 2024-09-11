package com.example.inventariodaws

data class DataItem(
    val noEmpleado: String,
    val planta: String,
    val items: List<Item>
)

data class Item(
    val scannedCode: String,
    val quantity: Int
)

data class ApiResponse(
    val title: String,
    val message: String,
    val statusCode: Int
)
