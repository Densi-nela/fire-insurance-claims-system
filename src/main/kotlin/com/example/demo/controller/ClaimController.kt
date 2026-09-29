package com.example.demo.controller

import com.example.demo.dto.ClaimRequest
import com.example.demo.dto.ClaimResponse
import com.example.demo.dto.ReviewRequest
import com.example.demo.dto.AttachmentResponse
import com.example.demo.security.RequiresRole
import com.example.demo.model.Claim
import com.example.demo.repository.ClaimRepository
import com.example.demo.repository.PolicyRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/claims")
class ClaimController(
    private val claimRepository: ClaimRepository,
    private val policyRepository: PolicyRepository
) {

    // Helper: Map a Database Claim Entity to a flat clean ClaimResponse DTO
    private fun mapToResponse(claim: Claim): ClaimResponse {
        val attachmentsDto = claim.attachments.map { attachment ->
            AttachmentResponse(
                id = attachment.id,
                fileName = attachment.fileName,
                fileType = attachment.fileType,
                fileSize = attachment.fileSize,
                downloadUrl = "/api/attachments/${attachment.id}",
                uploadedAt = attachment.uploadedAt
            )
        }
        return ClaimResponse(
            id = claim.id,
            policyNumber = claim.policy.policyNumber,
            claimantName = claim.policy.customer.name,
            propertyAddress = claim.policy.propertyAddress,
            causeOfFire = claim.causeOfFire,
            estimatedPropertyDamage = claim.estimatedPropertyDamage,
            estimatedContentDamage = claim.estimatedContentDamage,
            totalEstimatedDamage = claim.estimatedPropertyDamage + claim.estimatedContentDamage,
            isLivable = claim.isLivable,
            status = claim.status,
            submissionDate = claim.submissionDate,
            reviewerNotes = claim.reviewerNotes,
            attachments = attachmentsDto
        )
    }

    // 1. GET /api/claims - Fetch and dynamically filter registered claims
    @GetMapping
    @RequiresRole(["CUSTOMER", "ADJUSTER"])
    fun getAllClaims(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) policyNumber: String?,
        @RequestParam(required = false) minDamage: Double?,
        @RequestParam(required = false) maxDamage: Double?,
        request: HttpServletRequest
    ): ResponseEntity<Any> {
        val currentUserEmail = request.getAttribute("currentUserEmail") as? String
        val currentUserRole = request.getAttribute("currentUserRole") as? String

        val allFilteredClaims = claimRepository.findByFilters(status, policyNumber, minDamage, maxDamage)
        
        val claims = if (currentUserRole == "CUSTOMER") {
            // Production Security Rule: Customers can ONLY see claims associated with policies they own!
            if (currentUserEmail == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Unauthorized: User context missing."))
            }
            allFilteredClaims.filter { it.policy.customer.email == currentUserEmail }
        } else {
            // Adjusters can see all filtered claims
            allFilteredClaims
        }

        val dtoList = claims.map { mapToResponse(it) }
        return ResponseEntity.ok(dtoList)
    }

    // 2. GET /api/claims/{id} - Fetch a single claim by its ID
    @GetMapping("/{id}")
    fun getClaimById(@PathVariable id: Long): ResponseEntity<Any> {
        val claimOptional = claimRepository.findById(id)
        if (claimOptional.isEmpty) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Claim not found with ID: $id"))
        }
        return ResponseEntity.ok(mapToResponse(claimOptional.get()))
    }

    // 3. POST /api/claims - Submit a brand new fire claim form with auto-validation
    @PostMapping
    @RequiresRole(["CUSTOMER"])
    fun submitClaim(@RequestBody @Valid request: ClaimRequest): ResponseEntity<Any> {
        // Find the policy in SQLite
        val policy = policyRepository.findByPolicyNumber(request.policyNumber)
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Submission failed: Policy Number '${request.policyNumber}' does not exist in our records."))

        // Business Rule 1: Validate against Policy Coverage Limit
        val totalDamage = request.estimatedPropertyDamage + request.estimatedContentDamage
        if (totalDamage > policy.coverageLimit) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Submission rejected: Total estimated damage ($${totalDamage}) exceeds your policy coverage limit of ($${policy.coverageLimit})."))
        }

        // Business Rule 2: Auto-Approval for Small Claims (Total damage <= $2,000)
        val finalStatus = if (totalDamage <= 2000.00) "APPROVED" else "SUBMITTED"

        // Create the claim entity
        val claim = Claim(
            causeOfFire = request.causeOfFire,
            estimatedPropertyDamage = request.estimatedPropertyDamage,
            estimatedContentDamage = request.estimatedContentDamage,
            isLivable = request.isLivable,
            status = finalStatus,
            policy = policy
        )

        // Save claim to SQLite database
        val savedClaim = claimRepository.save(claim)

        // Return the clean response DTO with Status 201 (CREATED)
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(savedClaim))
    }

    // 4. PUT /api/claims/{id}/review - Review a claim (Approve or Reject with notes)
    @PutMapping("/{id}/review")
    @RequiresRole(["ADJUSTER"])
    fun reviewClaim(
        @PathVariable id: Long,
        @RequestBody @Valid request: ReviewRequest
    ): ResponseEntity<Any> {
        val claimOptional = claimRepository.findById(id)
        if (claimOptional.isEmpty) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Claim not found with ID: $id"))
        }

        val claim = claimOptional.get()

        // State Machine Rule: Cannot review an already settled claim (APPROVED or REJECTED)
        if (claim.status == "APPROVED" || claim.status == "REJECTED") {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Review rejected: Claim with ID $id has already been settled and is in status: ${claim.status}."))
        }

        // Apply state changes
        claim.status = request.status
        claim.reviewerNotes = request.reviewerNotes

        // Save back to SQLite database
        val updatedClaim = claimRepository.save(claim)

        return ResponseEntity.ok(mapToResponse(updatedClaim))
    }
}
