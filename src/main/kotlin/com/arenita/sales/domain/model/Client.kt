package com.arenita.sales.domain.model

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "clients")
data class Client(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false)
    val companyId: UUID,

    @Column(nullable = false)
    val name: String,

    val ruc: String? = null,
    val address: String? = null,
    val city: String? = null,
    val country: String? = null,
    val email: String? = null,
    val phone: String? = null,

    @Enumerated(EnumType.STRING)
    val clientType: ClientType = ClientType.REGULAR,

    val active: Boolean = true,
    val createdAt: Instant = Instant.now()
)

enum class ClientType { NEW, REGULAR }
