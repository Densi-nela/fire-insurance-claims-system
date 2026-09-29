package com.example.demo.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class PolicyRequest(
    @field:NotBlank(message = "Policy number is required.")
    val policyNumber: String,

    @field:NotBlank(message = "Property address is required.")
    val propertyAddress: String,

    @field:NotNull(message = "Coverage limit is required.")
    @field:Positive(message = "Coverage limit must be greater than zero.")
    val coverageLimit: Double,

    @field:NotNull(message = "Customer ID is required.")
    val customerId: Long
)
