package com.arenita.sales.infrastructure.mcp

import com.arenita.sales.application.CompanyService
import com.arenita.sales.application.ProformaService
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

/**
 * MCP (Model Context Protocol) endpoint.
 * Allows AI agents to interact with the sales system via structured tool calls.
 * 
 * Compatible with OpenClaw's mcporter skill and any MCP-compliant client.
 */
@RestController
@RequestMapping("/api/v1/mcp")
@Tag(name = "MCP", description = "Model Context Protocol for AI agent integration")
class McpController(
    private val proformaService: ProformaService,
    private val companyService: CompanyService,
    private val mapper: ObjectMapper
) {
    data class McpRequest(val method: String, val params: Map<String, Any?> = emptyMap())
    data class McpResponse(val result: Any?, val error: String? = null)
    data class McpToolDefinition(val name: String, val description: String, val inputSchema: Map<String, Any>)

    @GetMapping("/tools")
    @Operation(summary = "List available MCP tools")
    fun listTools(): List<McpToolDefinition> = listOf(
        McpToolDefinition(
            name = "list_proformas",
            description = "List proformas for a company. Filter by status and date range.",
            inputSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "company_id" to mapOf("type" to "string", "description" to "Company UUID"),
                    "status" to mapOf("type" to "string", "enum" to listOf("GENERATED", "APPROVED", "DELIVERED", "MAINTENANCE_DONE")),
                    "from" to mapOf("type" to "string", "format" to "date"),
                    "to" to mapOf("type" to "string", "format" to "date")
                ),
                "required" to listOf("company_id")
            )
        ),
        McpToolDefinition(
            name = "get_proforma",
            description = "Get full details of a proforma by ID, including all line items.",
            inputSchema = mapOf(
                "type" to "object",
                "properties" to mapOf("id" to mapOf("type" to "string", "description" to "Proforma UUID")),
                "required" to listOf("id")
            )
        ),
        McpToolDefinition(
            name = "update_proforma_status",
            description = "Update the status of a proforma (e.g., approve, mark as delivered).",
            inputSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "id" to mapOf("type" to "string", "description" to "Proforma UUID"),
                    "status" to mapOf("type" to "string", "enum" to listOf("GENERATED", "APPROVED", "DELIVERED", "MAINTENANCE_DONE")),
                    "sub_status" to mapOf("type" to "string", "enum" to listOf("SENT_AWAITING_APPROVAL", "REJECTED_NO_RESPONSE", "AWAITING_DISPATCH_CONFIRMATION", "MAINTENANCE_SCHEDULED")),
                    "approval_date" to mapOf("type" to "string", "format" to "date"),
                    "delivery_date" to mapOf("type" to "string", "format" to "date"),
                    "invoice_number" to mapOf("type" to "string"),
                    "observations" to mapOf("type" to "string")
                ),
                "required" to listOf("id", "status")
            )
        ),
        McpToolDefinition(
            name = "get_sales_summary",
            description = "Get sales summary for a company (totals, by status, by type).",
            inputSchema = mapOf(
                "type" to "object",
                "properties" to mapOf("company_id" to mapOf("type" to "string", "description" to "Company UUID")),
                "required" to listOf("company_id")
            )
        ),
        McpToolDefinition(
            name = "list_companies",
            description = "List all registered companies.",
            inputSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())
        )
    )

    @PostMapping("/call")
    @Operation(summary = "Execute an MCP tool call")
    fun callTool(@RequestBody req: McpRequest): McpResponse {
        return try {
            val result = when (req.method) {
                "list_proformas" -> {
                    val companyId = UUID.fromString(req.params["company_id"] as String)
                    val status = req.params["status"] as String?
                    val from = (req.params["from"] as String?)?.let { LocalDate.parse(it) }
                    val to = (req.params["to"] as String?)?.let { LocalDate.parse(it) }
                    proformaService.listByCompany(companyId, status, from, to)
                }
                "get_proforma" -> proformaService.getProforma(UUID.fromString(req.params["id"] as String))
                "update_proforma_status" -> {
                    val id = UUID.fromString(req.params["id"] as String)
                    proformaService.updateStatus(id, com.arenita.sales.api.dto.UpdateStatusRequest(
                        status = req.params["status"] as String,
                        subStatus = req.params["sub_status"] as String?,
                        approvalDate = (req.params["approval_date"] as String?)?.let { LocalDate.parse(it) },
                        deliveryDate = (req.params["delivery_date"] as String?)?.let { LocalDate.parse(it) },
                        invoiceNumber = req.params["invoice_number"] as String?,
                        observations = req.params["observations"] as String?
                    ))
                }
                "get_sales_summary" -> proformaService.getSummary(UUID.fromString(req.params["company_id"] as String))
                "list_companies" -> companyService.listAll()
                else -> throw IllegalArgumentException("Unknown method: ${req.method}")
            }
            McpResponse(result = result)
        } catch (e: Exception) {
            McpResponse(result = null, error = e.message)
        }
    }
}
