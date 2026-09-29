package com.example.demo.model

import jakarta.persistence.*

@Entity
@Table(name = "customers")
class Customer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(name = "phone_number")
    var phoneNumber: String? = null,

    @Column(nullable = false)
    var role: String = "CUSTOMER", // e.g. CUSTOMER, ADJUSTER

    @Column(nullable = false)
    var password: String = "password123", // Default password for testing

    @OneToMany(mappedBy = "customer", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var policies: List<Policy> = mutableListOf()
)
