package com.arenita.sales.domain.repository

import com.arenita.sales.domain.model.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate
import java.util.UUID

interface CompanyRepository : JpaRepository<Company, UUID> {
    fun findByRuc(ruc: String): Company?
}

interface SalesAgentRepository : JpaRepository<SalesAgent, UUID> {
    fun findByCompanyId(companyId: UUID): List<SalesAgent>
    fun findByTelegramId(telegramId: String): SalesAgent?
}

interface ClientRepository : JpaRepository<Client, UUID> {
    fun findByCompanyId(companyId: UUID): List<Client>
    fun findByRuc(ruc: String): Client?
    fun findByCompanyIdAndNameContainingIgnoreCase(companyId: UUID, name: String): List<Client>
}

interface ProformaRepository : JpaRepository<Proforma, UUID> {
    fun findByCompanyId(companyId: UUID): List<Proforma>
    fun findByCompanyIdAndStatus(companyId: UUID, status: ProformaStatus): List<Proforma>
    fun findByProformaNumberAndCompanyId(number: String, companyId: UUID): List<Proforma>
    fun findBySalesAgentId(agentId: UUID): List<Proforma>
    fun findByClientId(clientId: UUID): List<Proforma>

    @Query("SELECT p FROM Proforma p WHERE p.companyId = :companyId AND p.requestDate BETWEEN :from AND :to")
    fun findByCompanyIdAndDateRange(companyId: UUID, from: LocalDate, to: LocalDate): List<Proforma>

    @Query("SELECT p.status, COUNT(p) FROM Proforma p WHERE p.companyId = :companyId GROUP BY p.status")
    fun countByCompanyIdGroupByStatus(companyId: UUID): List<Array<Any>>

    @Query("SELECT p.proformaType, COUNT(p) FROM Proforma p WHERE p.companyId = :companyId GROUP BY p.proformaType")
    fun countByCompanyIdGroupByType(companyId: UUID): List<Array<Any>>
}

interface ProformaItemRepository : JpaRepository<ProformaItem, UUID> {
    fun findByProformaId(proformaId: UUID): List<ProformaItem>
    fun deleteByProformaId(proformaId: UUID)
}
