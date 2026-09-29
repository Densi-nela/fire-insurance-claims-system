package com.example.demo.dto

data class PolicyResponse(
    val id: Long?,
    val policyNumber: String,
    val propertyAddress: String,
    val coverageLimit: Double,
    val customerId: Long?,
    val customerName: String,
    val customerEmail: String? = null
)
