package com.example.demo.controller

import com.example.demo.dto.LoginRequest
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var customerRepository: CustomerRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private fun createTestUser(email: String, role: String): Customer {
        val customer = Customer(
            name = "Test User",
            email = email,
            phoneNumber = "555-9000",
            role = role,
            password = passwordEncoder.encode("testPassword")!!
        )
        return customerRepository.save(customer)
    }

    @Test
    fun `POST api auth login succeeds with correct credentials`() {
        val uniqueEmail = "user.${UUID.randomUUID()}@example.com"
        createTestUser(uniqueEmail, "CUSTOMER")

        val request = LoginRequest(
            email = uniqueEmail,
            password = "testPassword"
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.email").value(uniqueEmail))
            .andExpect(jsonPath("$.role").value("CUSTOMER"))
            .andExpect(jsonPath("$.message").value("Login successful!"))
    }

    @Test
    fun `POST api auth login returns 401 Unauthorized for wrong password`() {
        val uniqueEmail = "user.${UUID.randomUUID()}@example.com"
        createTestUser(uniqueEmail, "CUSTOMER")

        val request = LoginRequest(
            email = uniqueEmail,
            password = "wrongPassword"
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error", containsString("Invalid email or password")))
    }

    @Test
    fun `POST api auth login returns 401 Unauthorized for non-existent email`() {
        val request = LoginRequest(
            email = "non-existent@example.com",
            password = "anyPassword"
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error", containsString("Invalid email or password")))
    }
}
