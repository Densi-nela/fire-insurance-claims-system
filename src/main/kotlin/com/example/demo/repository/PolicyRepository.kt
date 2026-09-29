package com.example.demo.repository

import com.example.demo.model.Policy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PolicyRepository : JpaRepository<Policy, Long> {
    fun findByPolicyNumber(policyNumber: String): Policy?
    fun findByCustomer_Id(customerId: Long): List<Policy>
}
