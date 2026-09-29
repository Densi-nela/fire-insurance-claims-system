package com.example.demo.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

data class ClaimRequest(
    @field:NotBlank(message = "Policy number is required.")
    val policyNumber: String,

    @field:NotBlank(message = "Cause of fire is required.")
    @field:Size(min = 10, message = "Cause of fire description must be at least 10 characters long.")
    val causeOfFire: String,

    @field:PositiveOrZero(message = "Estimated property damage cannot be negative.")
    val estimatedPropertyDamage: Double,

    @field:PositiveOrZero(message = "Estimated content damage cannot be negative.")
    val estimatedContentDamage: Double,

    val isLivable: Boolean
)
