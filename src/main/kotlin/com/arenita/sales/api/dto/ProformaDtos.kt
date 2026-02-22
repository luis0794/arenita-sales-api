package com.arenita.sales.api.dto

import com.arenita.sales.domain.model.*
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// === Proforma DTOs ===

/** Minimal payload from mobile — just the PDF + minimal metadata */
data class CreateProformaFromPdfRequest(
    @field:NotBlank val companyId: String,
    val salesAgentId: String? = null,
    val requestDate: LocalDate? = null,
    val requestChannel: String? = null,
    val proformaType: String? = null,
    val clientType: String? = null,
    val promotionApplied: String? = null,
    val observations: String? = null
    // PDF is sent as multipart file
)

/** Full manual creation */
data class CreateProformaRequest(
    @field:NotBlank val companyId: String,
    @field:NotBlank val proformaNumber: String,
    val requestDate: LocalDate? = null,
    val issueDate: LocalDate? = null,
    val validUntil: LocalDate? = null,
    val salesAgentId: String? = null,
    val clientName: String? = null,
    val clientRuc: String? = null,
    val clientAddress: String? = null,
    val clientCity: String? = null,
    val vendorName: String? = null,
    val proformaType: String = "REAGENTS",
    val requestChannel: String? = null,
    val clientType: String = "REGULAR",
    val promotionApplied: String? = null,
    val paymentTerms: String? = null,
    val deliveryDays: Int? = null,
    val issuerRuc: String? = null,
    val observations: String? = null,
    val items: List<ProformaItemRequest> = emptyList()
)

data class ProformaItemRequest(
    val quantity: Int = 1,
    @field:NotBlank val description: String,
    val lot: String? = null,
    val expirationDate: LocalDate? = null,
    val discountPercent: BigDecimal = BigDecimal.ZERO,
    val unitPrice: BigDecimal = BigDecimal.ZERO,
    val totalPrice: BigDecimal = BigDecimal.ZERO
)

data class UpdateStatusRequest(
    @field:NotBlank val status: String,
    val subStatus: String? = null,
    val approvalDate: LocalDate? = null,
    val deliveryDate: LocalDate? = null,
    val invoiceNumber: String? = null,
    val observations: String? = null
)

// === Response DTOs ===

data class ProformaResponse(
    val id: UUID,
    val companyId: UUID,
    val proformaNumber: String,
    val version: Int,
    val requestDate: LocalDate?,
    val issueDate: LocalDate?,
    val validUntil: LocalDate?,
    val approvalDate: LocalDate?,
    val deliveryDate: LocalDate?,
    val salesAgentName: String?,
    val clientName: String?,
    val clientRuc: String?,
    val vendorName: String?,
    val proformaType: String,
    val requestChannel: String?,
    val clientType: String,
    val promotionApplied: String?,
    val status: String,
    val subStatus: String?,
    val paymentTerms: String?,
    val deliveryDays: Int?,
    val subtotal: BigDecimal,
    val discount: BigDecimal,
    val subtotalWithDiscount: BigDecimal,
    val tax: BigDecimal,
    val total: BigDecimal,
    val invoiceNumber: String?,
    val observations: String?,
    val items: List<ProformaItemResponse>,
    val createdAt: Instant
)

data class ProformaItemResponse(
    val id: UUID,
    val quantity: Int,
    val description: String,
    val lot: String?,
    val expirationDate: LocalDate?,
    val discountPercent: BigDecimal,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal
)

data class ProformaSummary(
    val totalProformas: Long,
    val byStatus: Map<String, Long>,
    val byType: Map<String, Long>,
    val totalAmount: BigDecimal
)

// === Company DTOs ===
data class CreateCompanyRequest(
    @field:NotBlank val ruc: String,
    @field:NotBlank val name: String,
    val tradeName: String? = null,
    val address: String? = null,
    val city: String? = null,
    val country: String = "EC",
    val industry: String = "DIAGNOSTICS"
)

data class CompanyResponse(
    val id: UUID, val ruc: String, val name: String,
    val tradeName: String?, val address: String?,
    val city: String?, val country: String,
    val industry: String, val active: Boolean
)

// === Webhook ===
data class WebhookProformaEvent(
    val eventType: String, // proforma.created, proforma.updated, proforma.status_changed
    val companyRuc: String,
    val proformaNumber: String,
    val data: Map<String, Any?> = emptyMap()
)

// === Mappers ===
fun Company.toResponse() = CompanyResponse(id!!, ruc, name, tradeName, address, city, country, industry.name, active)
