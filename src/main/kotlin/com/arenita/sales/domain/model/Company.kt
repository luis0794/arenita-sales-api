package com.arenita.sales.domain.model

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "companies")
data class Company(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false, unique = true)
    val ruc: String,

    @Column(nullable = false)
    val name: String,

    val tradeName: String? = null,
    val address: String? = null,
    val city: String? = null,
    val country: String = "EC",

    @Enumerated(EnumType.STRING)
    val industry: Industry = Industry.DIAGNOSTICS,

    val active: Boolean = true,
    val createdAt: Instant = Instant.now()
)

enum class Industry {
    DIAGNOSTICS, PHARMACEUTICAL, MEDICAL_DEVICES, LABORATORY, OTHER
}
