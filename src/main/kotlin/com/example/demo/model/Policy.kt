package com.example.demo.model

import jakarta.persistence.*

@Entity
@Table(name = "policies")
class Policy(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "policy_number", nullable = false, unique = true)
    val policyNumber: String,

    @Column(name = "property_address", nullable = false)
    val propertyAddress: String,

    @Column(name = "coverage_limit", nullable = false)
    val coverageLimit: Double,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    val customer: Customer
)
