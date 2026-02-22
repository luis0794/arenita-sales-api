package com.arenita.sales.infrastructure.web

import com.arenita.sales.application.ExportService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1/export")
@Tag(name = "Export", description = "Data export for Power BI and analytics")
class ExportController(private val exportService: ExportService) {

    @GetMapping("/proformas/{companyId}/csv")
    @Operation(summary = "Export proformas as CSV (one row per product, all fields)")
    fun exportCsv(
        @PathVariable companyId: UUID,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?
    ): ResponseEntity<ByteArray> {
        val csv = exportService.exportCsv(companyId, status, from, to)
        val bytes = csv.toByteArray(Charsets.UTF_8)
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) // UTF-8 BOM for Excel

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"proformas_export.csv\"")
            .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
            .body(bom + bytes)
    }

    @GetMapping("/proformas/{companyId}/json")
    @Operation(summary = "Export proformas as JSON (for Power BI REST connector)")
    fun exportJson(
        @PathVariable companyId: UUID,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?
    ): ResponseEntity<Any> {
        // Reuses ProformaService but returns flat format optimized for BI
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("message" to "Use /csv for now. JSON BI connector coming soon."))
    }
}
