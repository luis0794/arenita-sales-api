package com.arenita.sales.application

import com.arenita.sales.domain.model.*
import com.arenita.sales.domain.repository.*
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class ExportService(
    private val proformaRepo: ProformaRepository,
    private val itemRepo: ProformaItemRepository,
    private val clientRepo: ClientRepository,
    private val agentRepo: SalesAgentRepository
) {
    private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun exportCsv(companyId: UUID, status: String?, from: LocalDate?, to: LocalDate?): String {
        val proformas = when {
            status != null -> proformaRepo.findByCompanyIdAndStatus(companyId, ProformaStatus.valueOf(status.uppercase()))
            from != null && to != null -> proformaRepo.findByCompanyIdAndDateRange(companyId, from, to)
            else -> proformaRepo.findByCompanyId(companyId)
        }

        val header = listOf(
            "Proforma #", "Versión", "Fecha Solicitud", "Fecha Emisión", "Válida Hasta",
            "Vendedor", "Asesor/a Comercial", "Medio de Solicitud", "Tipo Proforma",
            "Tipo Cliente", "Promoción Aplicada", "Forma de Pago", "Días Entrega",
            "RUC Emisor", "Cliente", "Cédula/RUC Cliente", "Dirección Cliente", "Ciudad",
            "Cant.", "Descripción", "Lote", "Fecha Vencimiento", "% Descuento",
            "Precio Unitario (USD)", "Precio Total (USD)", "Subtotal", "Descuento Total",
            "Subtotal con Descuento", "Impuesto", "Total Proforma",
            "Estado", "Subestado", "Fecha Aprobación", "Fecha Entrega/Realización",
            "# Factura", "Observaciones"
        ).joinToString(",")

        val rows = proformas.flatMap { p ->
            val items = itemRepo.findByProformaId(p.id!!)
            val agent = p.salesAgentId?.let { agentRepo.findById(it).orElse(null) }
            val client = p.clientId?.let { clientRepo.findById(it).orElse(null) }

            val statusDisplay = when (p.status) {
                ProformaStatus.GENERATED -> "Proforma generada"
                ProformaStatus.APPROVED -> "Proforma aprobada"
                ProformaStatus.DELIVERED -> "Pedido entregado"
                ProformaStatus.MAINTENANCE_DONE -> "Mantenimiento realizado"
            }
            val subStatusDisplay = when (p.subStatus) {
                ProformaSubStatus.SENT_AWAITING_APPROVAL -> "Enviada al cliente a la espera de aprobación"
                ProformaSubStatus.REJECTED_NO_RESPONSE -> "Rechazada o sin respuesta"
                ProformaSubStatus.AWAITING_DISPATCH_CONFIRMATION -> "Espera de confirmación de despacho"
                ProformaSubStatus.MAINTENANCE_SCHEDULED -> "Mantenimiento agendado"
                null -> ""
            }
            val typeDisplay = when (p.proformaType) {
                ProformaType.REAGENTS -> "Reactivos"
                ProformaType.EQUIPMENT -> "Equipos"
                ProformaType.SPARE_PARTS -> "Repuestos"
                ProformaType.MAINTENANCE -> "Mantenimiento"
            }
            val channelDisplay = when (p.requestChannel) {
                RequestChannel.EMAIL -> "Correo"
                RequestChannel.WHATSAPP -> "WhatsApp"
                RequestChannel.TELEGRAM -> "Telegram"
                RequestChannel.PHONE -> "Teléfono"
                RequestChannel.IN_PERSON -> "Presencial"
                null -> ""
            }
            val clientTypeDisplay = when (p.clientType) {
                ClientType.NEW -> "Nuevo"
                ClientType.REGULAR -> "Regular"
            }

            if (items.isEmpty()) {
                listOf(buildRow(p, null, agent, client, statusDisplay, subStatusDisplay, typeDisplay, channelDisplay, clientTypeDisplay))
            } else {
                items.map { item ->
                    buildRow(p, item, agent, client, statusDisplay, subStatusDisplay, typeDisplay, channelDisplay, clientTypeDisplay)
                }
            }
        }

        return (listOf(header) + rows).joinToString("\n")
    }

    private fun buildRow(
        p: Proforma, item: ProformaItem?, agent: SalesAgent?, client: Client?,
        statusDisplay: String, subStatusDisplay: String, typeDisplay: String,
        channelDisplay: String, clientTypeDisplay: String
    ): String {
        return listOf(
            p.proformaNumber,
            p.version.toString(),
            p.requestDate?.format(dateFmt) ?: "",
            p.issueDate?.format(dateFmt) ?: "",
            p.validUntil?.format(dateFmt) ?: "",
            p.vendorName ?: "",
            agent?.name ?: "",
            channelDisplay,
            typeDisplay,
            clientTypeDisplay,
            p.promotionApplied ?: "",
            p.paymentTerms ?: "",
            p.deliveryDays?.toString() ?: "",
            p.issuerRuc ?: "",
            client?.name ?: "",
            client?.ruc ?: "",
            csvEscape(client?.address ?: ""),
            client?.city ?: "",
            item?.quantity?.toString() ?: "",
            csvEscape(item?.description ?: ""),
            item?.lot ?: "",
            item?.expirationDate?.format(dateFmt) ?: "",
            item?.discountPercent?.toPlainString() ?: "",
            item?.unitPrice?.toPlainString() ?: "",
            item?.totalPrice?.toPlainString() ?: "",
            p.subtotal.toPlainString(),
            p.discount.toPlainString(),
            p.subtotalWithDiscount.toPlainString(),
            p.tax.toPlainString(),
            p.total.toPlainString(),
            statusDisplay,
            subStatusDisplay,
            p.approvalDate?.format(dateFmt) ?: "",
            p.deliveryDate?.format(dateFmt) ?: "",
            p.invoiceNumber ?: "",
            csvEscape(p.observations ?: "")
        ).joinToString(",")
    }

    private fun csvEscape(value: String): String =
        if (value.contains(",") || value.contains("\"") || value.contains("\n"))
            "\"${value.replace("\"", "\"\"")}\""
        else value
}
