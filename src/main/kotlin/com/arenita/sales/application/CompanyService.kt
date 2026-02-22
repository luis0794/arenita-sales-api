package com.arenita.sales.application

import com.arenita.sales.api.dto.*
import com.arenita.sales.domain.model.*
import com.arenita.sales.domain.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CompanyService(private val companyRepo: CompanyRepository) {

    @Transactional
    fun create(req: CreateCompanyRequest): CompanyResponse {
        val company = companyRepo.save(Company(
            ruc = req.ruc, name = req.name, tradeName = req.tradeName,
            address = req.address, city = req.city, country = req.country,
            industry = Industry.valueOf(req.industry.uppercase())
        ))
        return company.toResponse()
    }

    fun getById(id: UUID): CompanyResponse =
        companyRepo.findById(id).orElseThrow { NoSuchElementException("Company not found") }.toResponse()

    fun getByRuc(ruc: String): CompanyResponse =
        (companyRepo.findByRuc(ruc) ?: throw NoSuchElementException("Company not found")).toResponse()

    fun listAll(): List<CompanyResponse> = companyRepo.findAll().map { it.toResponse() }
}
