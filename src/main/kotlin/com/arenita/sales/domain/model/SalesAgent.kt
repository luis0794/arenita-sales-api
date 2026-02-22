package com.arenita.sales.domain.model

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "sales_agents")
data class SalesAgent(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false)
    val companyId: UUID,

    @Column(nullable = false)
    val name: String,

    val email: String? = null,
    val phone: String? = null,
    val telegramId: String? = null,

    @Enumerated(EnumType.STRING)
    val role: AgentRole = AgentRole.SALES,

    val active: Boolean = true,
    val createdAt: Instant = Instant.now()
)

enum class AgentRole { SALES, MANAGER, ADMIN }
