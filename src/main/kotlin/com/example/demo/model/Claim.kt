package com.example.demo.model

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "claims")
class Claim(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "cause_of_fire", nullable = false)
    val causeOfFire: String,

    @Column(name = "estimated_property_damage", nullable = false)
    val estimatedPropertyDamage: Double,

    @Column(name = "estimated_content_damage", nullable = false)
    val estimatedContentDamage: Double,

    @Column(name = "is_livable", nullable = false)
    val isLivable: Boolean,

    @Column(nullable = false)
    var status: String = "SUBMITTED", // e.g. SUBMITTED, INVESTIGATING, APPROVED, REJECTED

    @Column(name = "reviewer_notes")
    var reviewerNotes: String? = null,

    @Column(name = "submission_date", nullable = false)
    val submissionDate: LocalDate = LocalDate.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    val policy: Policy,

    @OneToMany(mappedBy = "claim", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var attachments: List<Attachment> = mutableListOf()
)
