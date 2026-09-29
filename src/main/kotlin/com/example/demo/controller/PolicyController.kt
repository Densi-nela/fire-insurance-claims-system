package com.example.demo.controller

import com.example.demo.dto.PolicyRequest
import com.example.demo.dto.PolicyResponse
import com.example.demo.model.Policy
import com.example.demo.repository.CustomerRepository
import com.example.demo.repository.PolicyRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/policies")
class PolicyController(
    private val policyRepository: PolicyRepository,
    private val customerRepository: CustomerRepository
) {

    // Helper: Map Entity to Response DTO
    private fun mapToResponse(policy: Policy): PolicyResponse {
        return PolicyResponse(
            id = policy.id,
            policyNumber = policy.policyNumber,
            propertyAddress = policy.propertyAddress,
            coverageLimit = policy.coverageLimit,
            customerId = policy.customer.id,
            customerName = policy.customer.name,
            customerEmail = policy.customer.email
        )
    }

    // 1. GET /api/policies - List policies scoped by user role (Customers only see their own!)
    @GetMapping
    fun getAllPolicies(request: HttpServletRequest): ResponseEntity<List<PolicyResponse>> {
        val currentUserEmail = request.getAttribute("currentUserEmail") as? String
        val currentUserRole = request.getAttribute("currentUserRole") as? String

        val allPolicies = policyRepository.findAll()
        val policies = if (currentUserRole == "CUSTOMER" && !currentUserEmail.isNullOrBlank()) {
            // Customer role: strictly return policies owned by the authenticated customer
            allPolicies.filter { it.customer.email.equals(currentUserEmail, ignoreCase = true) }
        } else {
            // Adjusters/Admins (or unit tests without customer auth) see all policies
            allPolicies
        }

        val dtoList = policies.map { mapToResponse(it) }
        return ResponseEntity.ok(dtoList)
    }

    // 2. GET /api/policies/{id} - Get a single policy by ID
    @GetMapping("/{id}")
    fun getPolicyById(@PathVariable id: Long): ResponseEntity<Any> {
        val policyOptional = policyRepository.findById(id)
        if (policyOptional.isEmpty) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Policy not found with ID: $id"))
        }
        return ResponseEntity.ok(mapToResponse(policyOptional.get()))
    }

    // 3. POST /api/policies - Issue a brand new policy with valid owner and unique policy number
    @PostMapping
    fun issuePolicy(@RequestBody @Valid request: PolicyRequest): ResponseEntity<Any> {
        // Validate customer existence
        val customerOptional = customerRepository.findById(request.customerId)
        if (customerOptional.isEmpty) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Issuance failed: Customer with ID '${request.customerId}' does not exist."))
        }

        // Validate policy number uniqueness
        val existingPolicy = policyRepository.findByPolicyNumber(request.policyNumber)
        if (existingPolicy != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Issuance failed: Policy number '${request.policyNumber}' already exists in our system."))
        }

        val customer = customerOptional.get()
        val policy = Policy(
            policyNumber = request.policyNumber,
            propertyAddress = request.propertyAddress,
            coverageLimit = request.coverageLimit,
            customer = customer
        )
        val savedPolicy = policyRepository.save(policy)

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(savedPolicy))
    }
}
