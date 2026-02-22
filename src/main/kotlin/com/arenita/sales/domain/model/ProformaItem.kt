package com.arenita.sales.domain.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "proforma_items")
data class ProformaItem(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false)
    val proformaId: UUID,

    val quantity: Int = 1,

    @Column(nullable = false)
    val description: String,

    val lot: String? = null,
    val expirationDate: LocalDate? = null,
    val discountPercent: BigDecimal = BigDecimal.ZERO,
    val unitPrice: BigDecimal = BigDecimal.ZERO,
    val totalPrice: BigDecimal = BigDecimal.ZERO,

    val sortOrder: Int = 0
)
