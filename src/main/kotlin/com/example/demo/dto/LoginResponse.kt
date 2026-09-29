package com.example.demo.dto

data class LoginResponse(
    val token: String,
    val email: String,
    val role: String,
    val message: String = "Login successful!"
)
