package com.example.demo.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class CustomerRequest(
    @field:NotBlank(message = "Customer name is required.")
    val name: String,

    @field:NotBlank(message = "Customer email is required.")
    @field:Email(message = "Customer email must be a valid email address.")
    val email: String,

    val phoneNumber: String?,

    val role: String? = "CUSTOMER",

    val password: String? = "password123"
)
