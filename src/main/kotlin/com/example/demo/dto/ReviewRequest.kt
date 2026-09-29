package com.example.demo.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class ReviewRequest(
    @field:NotBlank(message = "Review status is required.")
    @field:Pattern(regexp = "APPROVED|REJECTED", message = "Status must be either APPROVED or REJECTED.")
    val status: String,

    @field:NotBlank(message = "Reviewer notes are required.")
    @field:Size(min = 10, message = "Reviewer notes must be at least 10 characters long.")
    val reviewerNotes: String
)
