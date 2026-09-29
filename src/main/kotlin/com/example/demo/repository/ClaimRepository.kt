package com.example.demo.repository

import com.example.demo.model.Claim
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ClaimRepository : JpaRepository<Claim, Long> {
    // Spring Data automatically traverses the relationship to find claims by policy number!
    fun findByPolicyPolicyNumber(policyNumber: String): List<Claim>
    
    fun findByStatus(status: String): List<Claim>

    @Query("""
        SELECT c FROM Claim c 
        WHERE (:status IS NULL OR c.status = :status)
          AND (:policyNumber IS NULL OR c.policy.policyNumber = :policyNumber)
          AND (:minDamage IS NULL OR (c.estimatedPropertyDamage + c.estimatedContentDamage) >= :minDamage)
          AND (:maxDamage IS NULL OR (c.estimatedPropertyDamage + c.estimatedContentDamage) <= :maxDamage)
    """)
    fun findByFilters(
        @Param("status") status: String?,
        @Param("policyNumber") policyNumber: String?,
        @Param("minDamage") minDamage: Double?,
        @Param("maxDamage") maxDamage: Double?
    ): List<Claim>
}
