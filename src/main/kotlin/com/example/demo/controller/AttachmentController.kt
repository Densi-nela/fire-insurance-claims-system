package com.example.demo.controller

import com.example.demo.dto.AttachmentResponse
import com.example.demo.model.Attachment
import com.example.demo.repository.AttachmentRepository
import com.example.demo.repository.ClaimRepository
import com.example.demo.security.RequiresRole
import com.example.demo.service.FileStorageService
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api")
class AttachmentController(
    private val claimRepository: ClaimRepository,
    private val attachmentRepository: AttachmentRepository,
    private val fileStorageService: FileStorageService
) {

    // Helper: Map Attachment Entity to AttachmentResponse DTO
    private fun mapToResponse(attachment: Attachment): AttachmentResponse {
        return AttachmentResponse(
            id = attachment.id,
            fileName = attachment.fileName,
            fileType = attachment.fileType,
            fileSize = attachment.fileSize,
            downloadUrl = "/api/attachments/${attachment.id}",
            uploadedAt = attachment.uploadedAt
        )
    }

    // 1. POST /api/claims/{claimId}/attachments - Upload a supporting file
    @PostMapping("/claims/{claimId}/attachments", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @RequiresRole(["CUSTOMER"])
    fun uploadAttachment(
        @PathVariable claimId: Long,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<Any> {
        val claimOptional = claimRepository.findById(claimId)
        if (claimOptional.isEmpty) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Upload failed: Claim not found with ID: $claimId"))
        }

        if (file.isEmpty) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Upload failed: File is empty."))
        }

        val claim = claimOptional.get()
        val originalFileName = file.originalFilename ?: "unnamed_file"

        // Save file using pluggable FileStorageService (Local Disk or AWS S3 Bucket)
        val storedPath = fileStorageService.store(
            file.inputStream,
            originalFileName,
            file.contentType ?: "application/octet-stream"
        )

        // Save metadata to database
        val attachment = Attachment(
            fileName = originalFileName,
            fileType = file.contentType ?: "application/octet-stream",
            filePath = storedPath,
            fileSize = file.size,
            claim = claim
        )
        val savedAttachment = attachmentRepository.save(attachment)

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(savedAttachment))
    }

    // 2. GET /api/attachments/{id} - Securely stream / download the file
    @GetMapping("/attachments/{id}")
    fun getAttachmentFile(@PathVariable id: Long): ResponseEntity<Any> {
        val attachmentOptional = attachmentRepository.findById(id)
        if (attachmentOptional.isEmpty) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "File not found: Attachment metadata not found with ID: $id"))
        }

        val attachment = attachmentOptional.get()

        try {
            // Load file from pluggable storage as Spring Resource
            val resource: Resource = fileStorageService.loadAsResource(attachment.filePath)
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.fileType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${attachment.fileName}\"")
                .body(resource)
        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "File not found on server: ${e.message}"))
        }
    }
}
