package com.example.demo

import com.example.demo.model.Claim
import com.example.demo.model.Customer
import com.example.demo.model.Policy
import com.example.demo.repository.ClaimRepository
import com.example.demo.repository.CustomerRepository
import com.example.demo.repository.PolicyRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DatabaseSeeder(
    private val customerRepository: CustomerRepository,
    private val policyRepository: PolicyRepository,
    private val claimRepository: ClaimRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    @Transactional // Keeps the Hibernate session open so we can safely traverse relationships
    override fun run(vararg args: String) {
        println("\n=== INSURANCE DATABASE SEEDER STARTING ===")

        if (customerRepository.count() == 0L) {
            println("SQLite is empty. Seeding relational insurance data...")

            // 1. Create a Customer
            val customer = Customer(
                name = "Michael Scott",
                email = "michael.scott@dundermifflin.com",
                phoneNumber = "555-0199",
                role = "CUSTOMER",
                password = passwordEncoder.encode("password123")!!
            )
            val savedCustomer = customerRepository.save(customer)
            println("Saved Customer: ${savedCustomer.name}")

            // 2. Create a Policy for that Customer
            val policy = Policy(
                policyNumber = "POL-FIRE-9988",
                propertyAddress = "1725 Slough Avenue, Scranton, PA",
                coverageLimit = 350000.00,
                customer = savedCustomer
            )
            val savedPolicy = policyRepository.save(policy)
            savedCustomer.policies = listOf(savedPolicy) // Sync in-memory bidirectional relation to bypass Hibernate L1 cache
            println("Saved Policy: ${savedPolicy.policyNumber} for address ${savedPolicy.propertyAddress}")

            // 3. Create a House Fire Claim against that Policy
            val fireClaim = Claim(
                causeOfFire = "Toaster oven caught fire in the breakroom (started by Ryan the Temp).",
                estimatedPropertyDamage = 85000.00,
                estimatedContentDamage = 20000.00,
                isLivable = false,
                status = "UNDER_REVIEW",
                policy = savedPolicy
            )
            val savedClaim = claimRepository.save(fireClaim)
            println("Filed Fire Claim ID: ${savedClaim.id} for policy ${savedPolicy.policyNumber}")
            
            // 4. Create an Adjuster Account
            val adjuster = Customer(
                name = "David Wallace",
                email = "david.wallace@dundermifflin.com",
                phoneNumber = "555-0200",
                role = "ADJUSTER",
                password = passwordEncoder.encode("admin123")!!
            )
            customerRepository.save(adjuster)
            println("Saved Adjuster Account: ${adjuster.name} (${adjuster.email})")
            
            println("Successfully seeded all relational database tables!")
        } else {
            println("Database already contains records. Skipping seeding.")
        }

        // ===================================================================
        // Relational Query Demonstration
        // ===================================================================
        println("\n--- Fetching & Displaying Relational Data from SQLite ---")
        val customers = customerRepository.findAll()
        for (c in customers) {
            println("👤 Customer: ${c.name} | Email: ${c.email}")
            
            // Because of our bidirectional setup, we can access policies directly from the customer!
            val policies = c.policies
            if (policies.isEmpty()) {
                println("   (No active policies)")
            } else {
                for (p in policies) {
                    println("   📄 Policy #: ${p.policyNumber}")
                    println("      - Covered Address: ${p.propertyAddress}")
                    println("      - Coverage Limit: \$${p.coverageLimit}")
                    
                    // Let's fetch claims for this policy
                    val claims = claimRepository.findByPolicyPolicyNumber(p.policyNumber)
                    if (claims.isEmpty()) {
                        println("      - No claims registered under this policy.")
                    } else {
                        for (cl in claims) {
                            println("      🔥 [FIRE CLAIM DETAILS]")
                            println("         * Cause: ${cl.causeOfFire}")
                            println("         * Est. Property Damage: \$${cl.estimatedPropertyDamage}")
                            println("         * Est. Content Damage: \$${cl.estimatedContentDamage}")
                            println("         * Is House Livable?: ${if (cl.isLivable) "Yes" else "No"}")
                            println("         * Status: ${cl.status}")
                            println("         * Date: ${cl.submissionDate}")
                        }
                    }
                }
            }
        }
        println("=========================================================\n")
    }
}
