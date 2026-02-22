package com.arenita.sales.infrastructure.web

import com.arenita.sales.api.dto.*
import com.arenita.sales.application.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1/proformas")
@Tag(name = "Proformas", description = "Proforma management")
class ProformaController(
    private val proformaService: ProformaService,
    private val pdfParser: PdfParserService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create proforma manually")
    fun create(@Valid @RequestBody req: CreateProformaRequest) = proformaService.createProforma(req)

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create proforma from PDF upload (minimal data)")
    fun uploadPdf(
        @RequestParam("file") file: MultipartFile,
        @RequestParam companyId: String,
        @RequestParam(required = false) salesAgentId: String?,
        @RequestParam(required = false) requestDate: LocalDate?,
        @RequestParam(required = false) requestChannel: String?,
        @RequestParam(required = false) proformaType: String?,
        @RequestParam(required = false) clientType: String?,
        @RequestParam(required = false) promotionApplied: String?,
        @RequestParam(required = false) observations: String?
    ): ProformaResponse {
        val parsed = pdfParser.parsePdf(file, companyId)
        val enriched = parsed.copy(
            salesAgentId = salesAgentId ?: parsed.salesAgentId,
            requestDate = requestDate ?: parsed.requestDate,
            requestChannel = requestChannel ?: parsed.requestChannel,
            proformaType = proformaType ?: parsed.proformaType,
            clientType = clientType ?: parsed.clientType,
            promotionApplied = promotionApplied ?: parsed.promotionApplied,
            observations = observations ?: parsed.observations
        )
        return proformaService.createProforma(enriched)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get proforma by ID")
    fun get(@PathVariable id: UUID) = proformaService.getProforma(id)

    @GetMapping("/company/{companyId}")
    @Operation(summary = "List proformas by company")
    fun listByCompany(
        @PathVariable companyId: UUID,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?
    ) = proformaService.listByCompany(companyId, status, from, to)

    @GetMapping("/company/{companyId}/summary")
    @Operation(summary = "Get proforma summary for company")
    fun summary(@PathVariable companyId: UUID) = proformaService.getSummary(companyId)

    @PutMapping("/{id}/status")
    @Operation(summary = "Update proforma status")
    fun updateStatus(@PathVariable id: UUID, @Valid @RequestBody req: UpdateStatusRequest) =
        proformaService.updateStatus(id, req)

    @PostMapping("/{id}/version")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create new version of proforma")
    fun newVersion(@PathVariable id: UUID, @Valid @RequestBody req: CreateProformaRequest) =
        proformaService.createNewVersion(id, req)
}
