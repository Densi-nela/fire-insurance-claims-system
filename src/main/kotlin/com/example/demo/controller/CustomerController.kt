package com.example.demo.controller

import com.example.demo.dto.CustomerRequest
import com.example.demo.dto.CustomerResponse
import com.example.demo.model.Customer
import com.example.demo.repository.CustomerRepository
import com.example.demo.security.RequiresRole
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/customers")
class CustomerController(
    private val customerRepository: CustomerRepository,
    private val passwordEncoder: PasswordEncoder
) {

    // Helper: Map Entity to Response DTO
    private fun mapToResponse(customer: Customer): CustomerResponse {
        return CustomerResponse(
            id = customer.id,
            name = customer.name,
            email = customer.email,
            phoneNumber = customer.phoneNumber,
            policyNumbers = customer.policies.map { it.policyNumber },
            role = customer.role
        )
    }

    // 1. GET /api/customers - List all registered customers
    @GetMapping
    fun getAllCustomers(): ResponseEntity<List<CustomerResponse>> {
        val customers = customerRepository.findAll()
        val dtoList = customers.map { mapToResponse(it) }
        return ResponseEntity.ok(dtoList)
    }

    // 2. GET /api/customers/me - Get currently authenticated customer profile
    @GetMapping("/me")
    @RequiresRole(["CUSTOMER", "ADJUSTER"])
    fun getCurrentCustomer(request: HttpServletRequest): ResponseEntity<Any> {
        val currentUserEmail = request.getAttribute("currentUserEmail") as? String
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Unauthorized: User session missing."))

        val customer = customerRepository.findByEmail(currentUserEmail)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Customer profile not found."))

        return ResponseEntity.ok(mapToResponse(customer))
    }

    // 3. PUT /api/customers/me - Update currently authenticated customer contact info
    @PutMapping("/me")
    @RequiresRole(["CUSTOMER", "ADJUSTER"])
    fun updateCurrentCustomer(@RequestBody updateRequest: Map<String, String>, request: HttpServletRequest): ResponseEntity<Any> {
        val currentUserEmail = request.getAttribute("currentUserEmail") as? String
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Unauthorized: User session missing."))

        val customer = customerRepository.findByEmail(currentUserEmail)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Customer profile not found."))

        updateRequest["name"]?.let {
            if (it.isNotBlank()) customer.name = it.trim()
        }
        updateRequest["phoneNumber"]?.let {
            customer.phoneNumber = it.trim()
        }

        val savedCustomer = customerRepository.save(customer)
        return ResponseEntity.ok(mapToResponse(savedCustomer))
    }

    // 4. GET /api/customers/{id} - Get a single customer by ID
    @GetMapping("/{id:[0-9]+}")
    fun getCustomerById(@PathVariable id: Long): ResponseEntity<Any> {
        val customerOptional = customerRepository.findById(id)
        if (customerOptional.isEmpty) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Customer not found with ID: $id"))
        }
        return ResponseEntity.ok(mapToResponse(customerOptional.get()))
    }

    // 5. POST /api/customers - Register a new customer with unique email validation
    @PostMapping
    fun registerCustomer(@RequestBody @Valid request: CustomerRequest): ResponseEntity<Any> {
        // Validation check for unique email
        val existingCustomer = customerRepository.findByEmail(request.email)
        if (existingCustomer != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Registration failed: Email '${request.email}' is already registered."))
        }

        val customer = Customer(
            name = request.name,
            email = request.email,
            phoneNumber = request.phoneNumber,
            role = request.role ?: "CUSTOMER",
            password = passwordEncoder.encode(request.password ?: "password123")!!
        )
        val savedCustomer = customerRepository.save(customer)

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(savedCustomer))
    }
}
