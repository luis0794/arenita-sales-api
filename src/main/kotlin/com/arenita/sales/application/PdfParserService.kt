package com.arenita.sales.application

import com.arenita.sales.api.dto.CreateProformaRequest
import com.arenita.sales.api.dto.ProformaItemRequest
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class PdfParserService {
    private val log = LoggerFactory.getLogger(javaClass)
    private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun parsePdf(file: MultipartFile, companyId: String): CreateProformaRequest {
        val document = Loader.loadPDF(file.bytes)
        val text = PDFTextStripper().getText(document)
        document.close()

        log.info("PDF parsed, text length: ${text.length}")

        return CreateProformaRequest(
            companyId = companyId,
            proformaNumber = extractField(text, "Proforma#:", "\\d+") ?: "UNKNOWN",
            issueDate = extractDate(text, "Fecha emisión:"),
            validUntil = extractDate(text, "Valida hasta:"),
            vendorName = extractField(text, "VENDEDOR:", "[A-Za-zÀ-ÿ\\s]+"),
            clientName = extractField(text, "ATENCIÓN A:", "[A-Za-zÀ-ÿ\\s]+"),
            clientRuc = extractField(text, "Cedula/ruc:", "[\\d]+"),
            clientAddress = extractField(text, "Dirección", "[^\\n]+"),
            clientCity = extractField(text, "Ciudad:", "[A-Za-zÀ-ÿ\\s]+"),
            issuerRuc = extractField(text, "RUC:", "[\\d]+"),
            paymentTerms = extractField(text, "Forma de pago:", "[A-Za-zÀ-ÿ\\s]+"),
            deliveryDays = extractField(text, "Días entrega:", "\\d+")?.toIntOrNull(),
            items = parseItems(text),
            observations = extractField(text, "Observación:", "[^\\n]+")
        )
    }

    private fun parseItems(text: String): List<ProformaItemRequest> {
        // Simplified item parser — finds lines with quantity + description + price pattern
        val items = mutableListOf<ProformaItemRequest>()
        val lines = text.lines()

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            val match = Regex("^(\\d+)\\s+(.+?)\\s+\\.\\s+[\\d.]+\\s+USD\\s+([\\d.]+)\\s+USD\\s+([\\d.]+)").find(line)
            if (match != null) {
                val qty = match.groupValues[1].toInt()
                val desc = match.groupValues[2].trim()
                val unitPrice = BigDecimal(match.groupValues[3])
                val totalPrice = BigDecimal(match.groupValues[4])

                var lot: String? = null
                var expDate: LocalDate? = null
                // Check next line for lot/expiry
                if (i + 1 < lines.size) {
                    val nextLine = lines[i + 1].trim()
                    lot = Regex("Lote:\\s*([\\w-]+)").find(nextLine)?.groupValues?.get(1)
                    val expStr = Regex("Fecha vcto:\\s*([\\d/]+)").find(nextLine)?.groupValues?.get(1)
                    expDate = expStr?.let { runCatching { LocalDate.parse(it, dateFormat) }.getOrNull() }
                    if (lot != null) i++
                }

                items.add(ProformaItemRequest(
                    quantity = qty, description = desc, lot = lot,
                    expirationDate = expDate, unitPrice = unitPrice, totalPrice = totalPrice
                ))
            }
            i++
        }
        return items
    }

    private fun extractField(text: String, label: String, pattern: String): String? {
        val regex = Regex("$label\\s*($pattern)", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.get(1)?.trim()
    }

    private fun extractDate(text: String, label: String): LocalDate? {
        val dateStr = extractField(text, label, "[\\d/]+") ?: return null
        return runCatching { LocalDate.parse(dateStr, dateFormat) }.getOrNull()
    }
}
