package com.example.demo.dto

import java.time.LocalDate

data class ClaimResponse(
    val id: Long?,
    val policyNumber: String,
    val claimantName: String,
    val propertyAddress: String,
    val causeOfFire: String,
    val estimatedPropertyDamage: Double,
    val estimatedContentDamage: Double,
    val totalEstimatedDamage: Double,
    val isLivable: Boolean,
    val status: String,
    val submissionDate: LocalDate,
    val reviewerNotes: String?,
    val attachments: List<AttachmentResponse>
)
