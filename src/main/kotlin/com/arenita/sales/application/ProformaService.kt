package com.arenita.sales.application

import com.arenita.sales.api.dto.*
import com.arenita.sales.domain.model.*
import com.arenita.sales.domain.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class ProformaService(
    private val proformaRepo: ProformaRepository,
    private val itemRepo: ProformaItemRepository,
    private val clientRepo: ClientRepository,
    private val agentRepo: SalesAgentRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun createProforma(req: CreateProformaRequest): ProformaResponse {
        val companyId = UUID.fromString(req.companyId)

        // Find or create client
        var clientId: UUID? = null
        if (req.clientRuc != null) {
            val client = clientRepo.findByRuc(req.clientRuc) ?: clientRepo.save(Client(
                companyId = companyId, name = req.clientName ?: "Unknown",
                ruc = req.clientRuc, address = req.clientAddress,
                city = req.clientCity, clientType = ClientType.valueOf(req.clientType.uppercase())
            ))
            clientId = client.id
        }

        val items = req.items
        val subtotal = items.sumOf { it.totalPrice }
        val discount = items.filter { it.discountPercent > BigDecimal.ZERO }
            .sumOf { it.unitPrice.multiply(it.discountPercent).divide(BigDecimal(100)).multiply(BigDecimal(it.quantity)) }
        val subtotalWithDiscount = subtotal
        val tax = subtotalWithDiscount.multiply(BigDecimal("0.15")) // 15% IVA Ecuador
        val total = subtotalWithDiscount.add(tax)

        val proforma = proformaRepo.save(Proforma(
            companyId = companyId,
            proformaNumber = req.proformaNumber,
            requestDate = req.requestDate ?: LocalDate.now(),
            issueDate = req.issueDate,
            validUntil = req.validUntil,
            salesAgentId = req.salesAgentId?.let { UUID.fromString(it) },
            clientId = clientId,
            vendorName = req.vendorName,
            proformaType = ProformaType.valueOf(req.proformaType.uppercase()),
            requestChannel = req.requestChannel?.let { RequestChannel.valueOf(it.uppercase()) },
            clientType = ClientType.valueOf(req.clientType.uppercase()),
            promotionApplied = req.promotionApplied,
            paymentTerms = req.paymentTerms,
            deliveryDays = req.deliveryDays,
            subtotal = subtotal,
            discount = discount,
            subtotalWithDiscount = subtotalWithDiscount,
            tax = tax,
            total = total,
            issuerRuc = req.issuerRuc,
            observations = req.observations
        ))

        val savedItems = items.mapIndexed { idx, item ->
            itemRepo.save(ProformaItem(
                proformaId = proforma.id!!,
                quantity = item.quantity,
                description = item.description,
                lot = item.lot,
                expirationDate = item.expirationDate,
                discountPercent = item.discountPercent,
                unitPrice = item.unitPrice,
                totalPrice = item.totalPrice,
                sortOrder = idx
            ))
        }

        log.info("Proforma created: ${proforma.proformaNumber} company=${companyId}")
        return toResponse(proforma, savedItems)
    }

    @Transactional
    fun updateStatus(proformaId: UUID, req: UpdateStatusRequest): ProformaResponse {
        val proforma = proformaRepo.findById(proformaId).orElseThrow { NoSuchElementException("Proforma not found") }
        val updated = proforma.copy(
            status = ProformaStatus.valueOf(req.status.uppercase()),
            subStatus = req.subStatus?.let { ProformaSubStatus.valueOf(it.uppercase()) },
            approvalDate = req.approvalDate ?: proforma.approvalDate,
            deliveryDate = req.deliveryDate ?: proforma.deliveryDate,
            invoiceNumber = req.invoiceNumber ?: proforma.invoiceNumber,
            observations = req.observations ?: proforma.observations,
            updatedAt = Instant.now()
        )
        val saved = proformaRepo.save(updated)
        val items = itemRepo.findByProformaId(proformaId)
        log.info("Proforma status updated: ${saved.proformaNumber} → ${req.status}")
        return toResponse(saved, items)
    }

    fun getProforma(id: UUID): ProformaResponse {
        val p = proformaRepo.findById(id).orElseThrow { NoSuchElementException("Proforma not found") }
        return toResponse(p, itemRepo.findByProformaId(id))
    }

    fun listByCompany(companyId: UUID, status: String?, from: LocalDate?, to: LocalDate?): List<ProformaResponse> {
        val proformas = when {
            status != null -> proformaRepo.findByCompanyIdAndStatus(companyId, ProformaStatus.valueOf(status.uppercase()))
            from != null && to != null -> proformaRepo.findByCompanyIdAndDateRange(companyId, from, to)
            else -> proformaRepo.findByCompanyId(companyId)
        }
        return proformas.map { toResponse(it, itemRepo.findByProformaId(it.id!!)) }
    }

    fun getSummary(companyId: UUID): ProformaSummary {
        val proformas = proformaRepo.findByCompanyId(companyId)
        val byStatus = proformaRepo.countByCompanyIdGroupByStatus(companyId)
            .associate { (it[0] as ProformaStatus).name to it[1] as Long }
        val byType = proformaRepo.countByCompanyIdGroupByType(companyId)
            .associate { (it[0] as ProformaType).name to it[1] as Long }
        return ProformaSummary(
            totalProformas = proformas.size.toLong(),
            byStatus = byStatus,
            byType = byType,
            totalAmount = proformas.sumOf { it.total }
        )
    }

    @Transactional
    fun createNewVersion(proformaId: UUID, req: CreateProformaRequest): ProformaResponse {
        val original = proformaRepo.findById(proformaId).orElseThrow { NoSuchElementException("Proforma not found") }
        val versions = proformaRepo.findByProformaNumberAndCompanyId(original.proformaNumber, original.companyId)
        val newVersion = versions.maxOf { it.version } + 1

        val newReq = req.copy(proformaNumber = original.proformaNumber)
        val response = createProforma(newReq)

        // Update version on the newly created proforma
        val created = proformaRepo.findById(response.id).get()
        proformaRepo.save(created.copy(version = newVersion, parentProformaId = original.id))

        log.info("Proforma new version: ${original.proformaNumber} v${newVersion}")
        return getProforma(response.id)
    }

    private fun toResponse(p: Proforma, items: List<ProformaItem>): ProformaResponse {
        val agentName = p.salesAgentId?.let { agentRepo.findById(it).orElse(null)?.name }
        val client = p.clientId?.let { clientRepo.findById(it).orElse(null) }

        return ProformaResponse(
            id = p.id!!, companyId = p.companyId, proformaNumber = p.proformaNumber,
            version = p.version, requestDate = p.requestDate, issueDate = p.issueDate,
            validUntil = p.validUntil, approvalDate = p.approvalDate, deliveryDate = p.deliveryDate,
            salesAgentName = agentName, clientName = client?.name, clientRuc = client?.ruc,
            vendorName = p.vendorName, proformaType = p.proformaType.name,
            requestChannel = p.requestChannel?.name, clientType = p.clientType.name,
            promotionApplied = p.promotionApplied, status = p.status.name,
            subStatus = p.subStatus?.name, paymentTerms = p.paymentTerms,
            deliveryDays = p.deliveryDays, subtotal = p.subtotal, discount = p.discount,
            subtotalWithDiscount = p.subtotalWithDiscount, tax = p.tax, total = p.total,
            invoiceNumber = p.invoiceNumber, observations = p.observations,
            items = items.map { ProformaItemResponse(it.id!!, it.quantity, it.description, it.lot, it.expirationDate, it.discountPercent, it.unitPrice, it.totalPrice) },
            createdAt = p.createdAt
        )
    }
}
