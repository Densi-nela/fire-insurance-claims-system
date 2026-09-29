package com.example.demo.controller

import com.example.demo.dto.CustomerRequest
import com.example.demo.model.Customer
import com.example.demo.repository.CustomerRepository
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
class CustomerControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var customerRepository: CustomerRepository

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

    @Test
    fun `GET api customers returns all customers`() {
        mockMvc.perform(get("/api/customers"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    fun `GET api customers by id returns customer if found`() {
        val customer = createTestCustomer()

        mockMvc.perform(get("/api/customers/${customer.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(customer.id))
            .andExpect(jsonPath("$.name").value("Sarah Connor"))
            .andExpect(jsonPath("$.email").value(customer.email))
    }

    @Test
    fun `GET api customers by id returns 404 if not found`() {
        mockMvc.perform(get("/api/customers/999999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error", containsString("Customer not found")))
    }

    @Test
    fun `POST api customers registers new customer successfully`() {
        val uniqueEmail = "new.${UUID.randomUUID()}@example.com"
        val request = CustomerRequest(
            name = "John Connor",
            email = uniqueEmail,
            phoneNumber = "555-2000"
        )

        mockMvc.perform(
            post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("John Connor"))
            .andExpect(jsonPath("$.email").value(uniqueEmail))
    }

    @Test
    fun `POST api customers rejects registration if email is already taken`() {
        val customer = createTestCustomer()
        val request = CustomerRequest(
            name = "Another Name",
            email = customer.email, // Already used
            phoneNumber = "555-3000"
        )

        mockMvc.perform(
            post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("already registered")))
    }

    @Test
    fun `POST api customers returns validation errors for empty fields and bad email`() {
        val request = CustomerRequest(
            name = "", // Blank
            email = "not-an-email", // Malformed
            phoneNumber = "555"
        )

        mockMvc.perform(
            post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Validation failed"))
            .andExpect(jsonPath("$.details.name").value("Customer name is required."))
            .andExpect(jsonPath("$.details.email").value("Customer email must be a valid email address."))
    }
}
