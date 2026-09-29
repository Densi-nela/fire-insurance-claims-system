package com.example.demo.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "attachments")
class Attachment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "file_name", nullable = false)
    val fileName: String,

    @Column(name = "file_type", nullable = false)
    val fileType: String,

    @Column(name = "file_path", nullable = false)
    val filePath: String,

    @Column(name = "file_size", nullable = false)
    val fileSize: Long,

    @Column(name = "uploaded_at", nullable = false)
    val uploadedAt: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    val claim: Claim
)
