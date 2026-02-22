package com.arenita.sales.domain.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "proformas")
data class Proforma(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false)
    val companyId: UUID,

    @Column(nullable = false)
    val proformaNumber: String,

    val version: Int = 1,
    val parentProformaId: UUID? = null, // for versioning

    // Dates
    val requestDate: LocalDate? = null,
    val issueDate: LocalDate? = null,
    val validUntil: LocalDate? = null,
    val approvalDate: LocalDate? = null,
    val deliveryDate: LocalDate? = null,

    // People
    val salesAgentId: UUID? = null,
    val clientId: UUID? = null,
    val vendorName: String? = null,

    // Classification
    @Enumerated(EnumType.STRING)
    val proformaType: ProformaType = ProformaType.REAGENTS,

    @Enumerated(EnumType.STRING)
    val requestChannel: RequestChannel? = null,

    @Enumerated(EnumType.STRING)
    val clientType: ClientType = ClientType.REGULAR,

    val promotionApplied: String? = null,

    // Status
    @Enumerated(EnumType.STRING)
    val status: ProformaStatus = ProformaStatus.GENERATED,

    @Enumerated(EnumType.STRING)
    val subStatus: ProformaSubStatus? = ProformaSubStatus.SENT_AWAITING_APPROVAL,

    // Financials
    val paymentTerms: String? = null,
    val deliveryDays: Int? = null,
    val subtotal: BigDecimal = BigDecimal.ZERO,
    val discount: BigDecimal = BigDecimal.ZERO,
    val subtotalWithDiscount: BigDecimal = BigDecimal.ZERO,
    val tax: BigDecimal = BigDecimal.ZERO,
    val total: BigDecimal = BigDecimal.ZERO,

    // Invoice
    val invoiceNumber: String? = null,

    // Extra
    val observations: String? = null,
    val pdfFileKey: String? = null, // S3 key for uploaded PDF

    // Metadata
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),

    // Issuer info (from PDF)
    val issuerRuc: String? = null
)

enum class ProformaType { REAGENTS, EQUIPMENT, SPARE_PARTS, MAINTENANCE }

enum class RequestChannel { EMAIL, WHATSAPP, TELEGRAM, PHONE, IN_PERSON }

enum class ProformaStatus {
    GENERATED,       // Proforma generada
    APPROVED,        // Proforma aprobada
    DELIVERED,       // Pedido entregado (reactivos/equipos/repuestos)
    MAINTENANCE_DONE // Mantenimiento realizado
}

enum class ProformaSubStatus {
    SENT_AWAITING_APPROVAL,           // Enviada al cliente a la espera de aprobación
    REJECTED_NO_RESPONSE,             // Rechazada o sin respuesta
    AWAITING_DISPATCH_CONFIRMATION,   // Espera de confirmación de despacho
    MAINTENANCE_SCHEDULED             // Mantenimiento agendado
}
