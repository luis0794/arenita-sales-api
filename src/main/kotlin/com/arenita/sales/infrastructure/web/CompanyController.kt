package com.arenita.sales.infrastructure.web

import com.arenita.sales.api.dto.*
import com.arenita.sales.application.CompanyService
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/companies")
@Tag(name = "Companies", description = "Multi-company management")
class CompanyController(private val companyService: CompanyService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: CreateCompanyRequest) = companyService.create(req)

    @GetMapping
    fun list() = companyService.listAll()

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID) = companyService.getById(id)

    @GetMapping("/ruc/{ruc}")
    fun getByRuc(@PathVariable ruc: String) = companyService.getByRuc(ruc)
}
