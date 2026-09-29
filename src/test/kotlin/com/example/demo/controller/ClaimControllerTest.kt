package com.example.demo.controller

import com.example.demo.dto.ClaimRequest
import com.example.demo.dto.ReviewRequest
import com.example.demo.model.Customer
import com.example.demo.model.Policy
import com.example.demo.repository.ClaimRepository
import com.example.demo.repository.CustomerRepository
import com.example.demo.repository.PolicyRepository
import com.example.demo.security.JwtService
import tools.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClaimControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var customerRepository: CustomerRepository

    @Autowired
    private lateinit var policyRepository: PolicyRepository

    @Autowired
    private lateinit var claimRepository: ClaimRepository

    @Autowired
    private lateinit var jwtService: JwtService

    private val customerToken: String by lazy {
        "Bearer " + jwtService.generateToken("michael.scott@dundermifflin.com", "CUSTOMER")
    }

    private val adjusterToken: String by lazy {
        "Bearer " + jwtService.generateToken("david.wallace@dundermifflin.com", "ADJUSTER")
    }

    private fun createTestPolicy(coverageLimit: Double): Policy {
        val uniqueEmail = "test.${UUID.randomUUID()}@example.com"
        val uniquePolicyNumber = "POL-TEST-${UUID.randomUUID()}"
        
        val customer = Customer(
            name = "John Doe",
            email = uniqueEmail,
            phoneNumber = "555-0100"
        )
        val savedCustomer = customerRepository.save(customer)

        val policy = Policy(
            policyNumber = uniquePolicyNumber,
            propertyAddress = "456 Mockingbird Ln",
            coverageLimit = coverageLimit,
            customer = savedCustomer
        )
        return policyRepository.save(policy)
    }

    @Test
    fun `GET api claims returns all claims`() {
        mockMvc.perform(
            get("/api/claims")
                .header("Authorization", adjusterToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    fun `GET api claims returns 401 Unauthorized for missing token`() {
        mockMvc.perform(get("/api/claims"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error", containsString("Missing or malformed Authorization header")))
    }

    @Test
    fun `GET api claims returns 200 OK for customer role`() {
        mockMvc.perform(
            get("/api/claims")
                .header("Authorization", customerToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    fun `GET api claims by id returns claim if found`() {
        val policy = createTestPolicy(100000.0)
        val claim = com.example.demo.model.Claim(
            causeOfFire = "Electrical short in living room",
            estimatedPropertyDamage = 5000.0,
            estimatedContentDamage = 2000.0,
            isLivable = true,
            status = "SUBMITTED",
            policy = policy
        )
        val savedClaim = claimRepository.save(claim)

        mockMvc.perform(get("/api/claims/${savedClaim.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(savedClaim.id))
            .andExpect(jsonPath("$.causeOfFire").value("Electrical short in living room"))
            .andExpect(jsonPath("$.status").value("SUBMITTED"))
    }

    @Test
    fun `GET api claims by id returns 404 if not found`() {
        mockMvc.perform(get("/api/claims/999999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Claim not found with ID: 999999"))
    }

    @Test
    fun `POST api claims submits successfully and auto-approves small claims under 2000`() {
        val policy = createTestPolicy(10000.0)
        val request = ClaimRequest(
            policyNumber = policy.policyNumber,
            causeOfFire = "Toaster small flame",
            estimatedPropertyDamage = 1200.0,
            estimatedContentDamage = 500.0,
            isLivable = true
        )

        mockMvc.perform(
            post("/api/claims")
                .header("Authorization", customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.policyNumber").value(policy.policyNumber))
            .andExpect(jsonPath("$.status").value("APPROVED")) // Auto-approved because total <= 2000
            .andExpect(jsonPath("$.totalEstimatedDamage").value(1700.0))
    }

    @Test
    fun `POST api claims submits successfully with SUBMITTED status if over 2000 but within coverage limit`() {
        val policy = createTestPolicy(20000.0)
        val request = ClaimRequest(
            policyNumber = policy.policyNumber,
            causeOfFire = "Major stove fire",
            estimatedPropertyDamage = 4000.0,
            estimatedContentDamage = 1500.0,
            isLivable = false
        )

        mockMvc.perform(
            post("/api/claims")
                .header("Authorization", customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.policyNumber").value(policy.policyNumber))
            .andExpect(jsonPath("$.status").value("SUBMITTED")) // > 2000, so standard submitted status
            .andExpect(jsonPath("$.totalEstimatedDamage").value(5500.0))
    }

    @Test
    fun `POST api claims rejects submission if total damage exceeds policy coverage limit`() {
        val policy = createTestPolicy(5000.0)
        val request = ClaimRequest(
            policyNumber = policy.policyNumber,
            causeOfFire = "Entire living room burnt",
            estimatedPropertyDamage = 4500.0,
            estimatedContentDamage = 2000.0, // total = 6500 > 5000 limit
            isLivable = false
        )

        mockMvc.perform(
            post("/api/claims")
                .header("Authorization", customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("exceeds your policy coverage limit")))
    }

    @Test
    fun `POST api claims rejects submission if policy number does not exist`() {
        val request = ClaimRequest(
            policyNumber = "POL-NON-EXISTENT",
            causeOfFire = "Unknown cause of fire",
            estimatedPropertyDamage = 1000.0,
            estimatedContentDamage = 500.0,
            isLivable = true
        )

        mockMvc.perform(
            post("/api/claims")
                .header("Authorization", customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("does not exist in our records")))
    }

    @Test
    fun `POST api claims returns structured validation errors when fields are invalid`() {
        val request = ClaimRequest(
            policyNumber = "", // Blank policy
            causeOfFire = "Too short", // Length < 10
            estimatedPropertyDamage = -50.0, // Negative damage
            estimatedContentDamage = -10.0, // Negative damage
            isLivable = true
        )

        mockMvc.perform(
            post("/api/claims")
                .header("Authorization", customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Validation failed"))
            .andExpect(jsonPath("$.details.policyNumber").value("Policy number is required."))
            .andExpect(jsonPath("$.details.causeOfFire").value("Cause of fire description must be at least 10 characters long."))
            .andExpect(jsonPath("$.details.estimatedPropertyDamage").value("Estimated property damage cannot be negative."))
            .andExpect(jsonPath("$.details.estimatedContentDamage").value("Estimated content damage cannot be negative."))
    }

    @Test
    fun `PUT api claims review successfully approves a submitted claim`() {
        val policy = createTestPolicy(50000.0)
        val claim = com.example.demo.model.Claim(
            causeOfFire = "Water heater exploded",
            estimatedPropertyDamage = 15000.0,
            estimatedContentDamage = 5000.0,
            isLivable = true,
            status = "SUBMITTED",
            policy = policy
        )
        val savedClaim = claimRepository.save(claim)

        val reviewRequest = ReviewRequest(
            status = "APPROVED",
            reviewerNotes = "Damage fits the explosion report. Claim approved."
        )

        mockMvc.perform(
            put("/api/claims/${savedClaim.id}/review")
                .header("Authorization", adjusterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(savedClaim.id))
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.reviewerNotes").value("Damage fits the explosion report. Claim approved."))
    }

    @Test
    fun `PUT api claims review successfully rejects a submitted claim`() {
        val policy = createTestPolicy(50000.0)
        val claim = com.example.demo.model.Claim(
            causeOfFire = "Suspect kitchen fire",
            estimatedPropertyDamage = 8000.0,
            estimatedContentDamage = 2000.0,
            isLivable = true,
            status = "SUBMITTED",
            policy = policy
        )
        val savedClaim = claimRepository.save(claim)

        val reviewRequest = ReviewRequest(
            status = "REJECTED",
            reviewerNotes = "Investigation showed arson. Claim rejected."
        )

        mockMvc.perform(
            put("/api/claims/${savedClaim.id}/review")
                .header("Authorization", adjusterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(savedClaim.id))
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.reviewerNotes").value("Investigation showed arson. Claim rejected."))
    }

    @Test
    fun `PUT api claims review returns 400 Bad Request when trying to review an already settled claim`() {
        val policy = createTestPolicy(50000.0)
        val claim = com.example.demo.model.Claim(
            causeOfFire = "Approved small fire",
            estimatedPropertyDamage = 1000.0,
            estimatedContentDamage = 500.0,
            isLivable = true,
            status = "APPROVED", // Already settled
            policy = policy
        )
        val savedClaim = claimRepository.save(claim)

        val reviewRequest = ReviewRequest(
            status = "REJECTED",
            reviewerNotes = "Attempting to change a settled claim."
        )

        mockMvc.perform(
            put("/api/claims/${savedClaim.id}/review")
                .header("Authorization", adjusterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("already been settled")))
    }

    @Test
    fun `PUT api claims review returns 404 if claim does not exist`() {
        val reviewRequest = ReviewRequest(
            status = "APPROVED",
            reviewerNotes = "Valid notes for testing"
        )

        mockMvc.perform(
            put("/api/claims/999999/review")
                .header("Authorization", adjusterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewRequest))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Claim not found with ID: 999999"))
    }

    @Test
    fun `PUT api claims review returns 400 with structured errors for invalid status or short notes`() {
        val reviewRequest = ReviewRequest(
            status = "INVALID_STATUS", // Should be APPROVED or REJECTED
            reviewerNotes = "Short" // Should be >= 10 characters
        )

        mockMvc.perform(
            put("/api/claims/1/review")
                .header("Authorization", adjusterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Validation failed"))
            .andExpect(jsonPath("$.details.status").value("Status must be either APPROVED or REJECTED."))
            .andExpect(jsonPath("$.details.reviewerNotes").value("Reviewer notes must be at least 10 characters long."))
    }

    @Test
    fun `GET api claims returns filtered claims correctly`() {
        val policyA = createTestPolicy(20000.0)
        val policyB = createTestPolicy(15000.0)

        // Claim 1: Under Policy A, SUBMITTED, total damage = 6000
        val claim1 = com.example.demo.model.Claim(
            causeOfFire = "Policy A kitchen fire",
            estimatedPropertyDamage = 4000.0,
            estimatedContentDamage = 2000.0,
            isLivable = true,
            status = "SUBMITTED",
            policy = policyA
        )
        val savedClaim1 = claimRepository.save(claim1)

        // Claim 2: Under Policy B, APPROVED, total damage = 1500
        val claim2 = com.example.demo.model.Claim(
            causeOfFire = "Policy B small fire",
            estimatedPropertyDamage = 1000.0,
            estimatedContentDamage = 500.0,
            isLivable = true,
            status = "APPROVED",
            policy = policyB
        )
        val savedClaim2 = claimRepository.save(claim2)

        // 1. Filter by status = SUBMITTED
        mockMvc.perform(get("/api/claims").param("status", "SUBMITTED").header("Authorization", adjusterToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.id == ${savedClaim1.id})]").exists())
            .andExpect(jsonPath("$[?(@.id == ${savedClaim2.id})]").doesNotExist())

        // 2. Filter by status = APPROVED
        mockMvc.perform(get("/api/claims").param("status", "APPROVED").header("Authorization", adjusterToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.id == ${savedClaim2.id})]").exists())
            .andExpect(jsonPath("$[?(@.id == ${savedClaim1.id})]").doesNotExist())

        // 3. Filter by policy number = policyA.policyNumber
        mockMvc.perform(get("/api/claims").param("policyNumber", policyA.policyNumber).header("Authorization", adjusterToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.id == ${savedClaim1.id})]").exists())
            .andExpect(jsonPath("$[?(@.id == ${savedClaim2.id})]").doesNotExist())

        // 4. Filter by minDamage = 5000.0
        mockMvc.perform(get("/api/claims").param("minDamage", "5000.0").header("Authorization", adjusterToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.id == ${savedClaim1.id})]").exists())
            .andExpect(jsonPath("$[?(@.id == ${savedClaim2.id})]").doesNotExist())

        // 5. Filter by maxDamage = 2000.0
        mockMvc.perform(get("/api/claims").param("maxDamage", "2000.0").header("Authorization", adjusterToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.id == ${savedClaim2.id})]").exists())
            .andExpect(jsonPath("$[?(@.id == ${savedClaim1.id})]").doesNotExist())
            
        // 6. Combine multiple filters (status = APPROVED, maxDamage = 2000.0)
        mockMvc.perform(
            get("/api/claims")
                .param("status", "APPROVED")
                .param("maxDamage", "2000.0")
                .header("Authorization", adjusterToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.id == ${savedClaim2.id})]").exists())
            .andExpect(jsonPath("$[?(@.id == ${savedClaim1.id})]").doesNotExist())
    }
}
