package com.example.demo.controller

import com.example.demo.dto.PolicyRequest
import com.example.demo.model.Customer
import com.example.demo.model.Policy
import com.example.demo.repository.CustomerRepository
import com.example.demo.repository.PolicyRepository
import tools.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PolicyControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var customerRepository: CustomerRepository

    @Autowired
    private lateinit var policyRepository: PolicyRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private fun createTestCustomer(): Customer {
        val email = "cust.${UUID.randomUUID()}@example.com"
        val customer = Customer(
            name = "Sarah Connor",
            email = email,
            phoneNumber = "555-1000"
        )
        return customerRepository.save(customer)
    }

    private fun createTestPolicy(customer: Customer): Policy {
        val policyNumber = "POL-TEST-${UUID.randomUUID()}"
        val policy = Policy(
            policyNumber = policyNumber,
            propertyAddress = "123 Mockingbird Ln",
            coverageLimit = 150000.0,
            customer = customer
        )
        return policyRepository.save(policy)
    }

    @Test
    fun `GET api policies returns all policies`() {
        mockMvc.perform(get("/api/policies"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    fun `GET api policies by id returns policy if found`() {
        val customer = createTestCustomer()
        val policy = createTestPolicy(customer)

        mockMvc.perform(get("/api/policies/${policy.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(policy.id))
            .andExpect(jsonPath("$.policyNumber").value(policy.policyNumber))
            .andExpect(jsonPath("$.customerName").value("Sarah Connor"))
    }

    @Test
    fun `GET api policies by id returns 404 if not found`() {
        mockMvc.perform(get("/api/policies/999999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error", containsString("Policy not found")))
    }

    @Test
    fun `POST api policies issues new policy successfully`() {
        val customer = createTestCustomer()
        val uniquePolicyNumber = "POL-NEW-${UUID.randomUUID()}"
        val request = PolicyRequest(
            policyNumber = uniquePolicyNumber,
            propertyAddress = "789 Cyberdyne Systems Blvd",
            coverageLimit = 250000.00,
            customerId = customer.id!!
        )

        mockMvc.perform(
            post("/api/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.policyNumber").value(uniquePolicyNumber))
            .andExpect(jsonPath("$.customerId").value(customer.id))
            .andExpect(jsonPath("$.customerName").value("Sarah Connor"))
    }

    @Test
    fun `POST api policies returns 404 if customer does not exist`() {
        val uniquePolicyNumber = "POL-NEW-${UUID.randomUUID()}"
        val request = PolicyRequest(
            policyNumber = uniquePolicyNumber,
            propertyAddress = "789 Cyberdyne Systems Blvd",
            coverageLimit = 250000.00,
            customerId = 999999L // Non-existent customer
        )

        mockMvc.perform(
            post("/api/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error", containsString("Customer with ID '999999' does not exist")))
    }

    @Test
    fun `POST api policies rejects issuance if policy number is duplicate`() {
        val customer = createTestCustomer()
        val policy = createTestPolicy(customer)
        val request = PolicyRequest(
            policyNumber = policy.policyNumber, // Duplicate policy number
            propertyAddress = "789 Cyberdyne Systems Blvd",
            coverageLimit = 250000.00,
            customerId = customer.id!!
        )

        mockMvc.perform(
            post("/api/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("already exists in our system")))
    }

    @Test
    fun `POST api policies returns validation errors for blank fields and negative coverage`() {
        val request = PolicyRequest(
            policyNumber = "", // Blank
            propertyAddress = "", // Blank
            coverageLimit = -500.00, // Negative coverage
            customerId = 1L
        )

        mockMvc.perform(
            post("/api/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Validation failed"))
            .andExpect(jsonPath("$.details.policyNumber").value("Policy number is required."))
            .andExpect(jsonPath("$.details.propertyAddress").value("Property address is required."))
            .andExpect(jsonPath("$.details.coverageLimit").value("Coverage limit must be greater than zero."))
    }
}
