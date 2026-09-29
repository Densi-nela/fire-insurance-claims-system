package com.example.demo.controller

import com.example.demo.dto.LoginRequest
import com.example.demo.dto.LoginResponse
import com.example.demo.repository.CustomerRepository
import com.example.demo.security.JwtService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val customerRepository: CustomerRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder
) {

    @PostMapping("/login")
    fun login(@RequestBody @Valid request: LoginRequest): ResponseEntity<Any> {
        val customer = customerRepository.findByEmail(request.email)
        if (customer == null || !passwordEncoder.matches(request.password, customer.password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Login failed: Invalid email or password."))
        }

        // Generate the secure JWT token
        val token = jwtService.generateToken(customer.email, customer.role)

        val response = LoginResponse(
            token = token,
            email = customer.email,
            role = customer.role
        )
        return ResponseEntity.ok(response)
    }
}
