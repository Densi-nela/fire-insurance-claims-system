package com.example.demo.dto

data class CustomerResponse(
    val id: Long?,
    val name: String,
    val email: String,
    val phoneNumber: String?,
    val policyNumbers: List<String>,
    val role: String? = "CUSTOMER"
)
