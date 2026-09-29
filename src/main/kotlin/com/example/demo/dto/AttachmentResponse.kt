package com.example.demo.dto

import java.time.LocalDateTime

data class AttachmentResponse(
    val id: Long?,
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
    val downloadUrl: String,
    val uploadedAt: LocalDateTime
)
