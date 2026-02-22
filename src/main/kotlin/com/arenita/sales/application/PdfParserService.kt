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
    private val dateFormats = listOf(
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
    )
    
    // Ecuador cities to validate city extraction
    private val ecuadorianCities = setOf(
        "QUITO", "GUAYAQUIL", "CUENCA", "SANTO DOMINGO", "MANTA", "AMBATO",
        "PORTOVIEJO", "MACHALA", "LOJA", "RIOBAMBA", "LATACUNGA", "PUYO",
        "BABAHOYO", "ESMERALDAS", "IBARRA", "TULCAN", "COCA", "DAULE",
        "DURAN", "SAMBORONDON", "SALINAS", "MONTANITA", "OTAVALO",
        "ALOAG", "NAPO", "MORONA", "ZAMORA"
    )

    /**
     * Extract raw text from PDF for debugging
     */
    fun extractText(file: MultipartFile): String {
        val document = Loader.loadPDF(file.bytes)
        val text = PDFTextStripper().getText(document)
        document.close()
        return text
    }

    fun parsePdf(file: MultipartFile, companyId: String): CreateProformaRequest {
        val document = Loader.loadPDF(file.bytes)
        val text = PDFTextStripper().getText(document)
        document.close()

        log.info("=== PDF TEXT START ===")
        log.info(text)
        log.info("=== PDF TEXT END === (length: ${text.length})")

        val items = parseItems(text)
        log.info("Parsed ${items.size} items from PDF")

        // Calculate totals from items if we found any
        val subtotalFromItems = if (items.isNotEmpty()) items.sumOf { it.totalPrice } else null
        // Try to extract totals from PDF text directly
        val pdfSubtotal = extractMoney(text, "(?:Sub\\s*total|SUBTOTAL)\\s*(?:USD)?\\s*:?\\s*\\$?")
        val pdfTotal = extractMoney(text, "(?:TOTAL|Total)\\s*(?:USD)?\\s*:?\\s*\\$?")
        val pdfTax = extractMoney(text, "(?:I\\.?V\\.?A\\.?|IVA|Impuesto)\\s*(?:\\d+%)?\\s*(?:USD)?\\s*:?\\s*\\$?")
        val pdfDiscount = extractMoney(text, "(?:Descuento|DESCUENTO)\\s*(?:USD)?\\s*:?\\s*\\$?")

        log.info("Totals — items_subtotal: $subtotalFromItems, pdf_subtotal: $pdfSubtotal, pdf_total: $pdfTotal, pdf_tax: $pdfTax")

        return CreateProformaRequest(
            companyId = companyId,
            proformaNumber = extractProformaNumber(text) ?: "UNKNOWN",
            issueDate = extractDate(text, "(?:Fecha\\s*(?:de\\s*)?emisi[oó]n|Fecha)"),
            validUntil = extractDate(text, "(?:V[aá]lida?\\s*hasta|Vigencia)"),
            vendorName = null, // Vendor name comes from frontend
            clientName = extractClientName(text),
            clientRuc = extractField(text, "(?:C[eé]dula/?[Rr][Uu][Cc]|RUC\\s*(?:Cliente)?)", "[\\d-]+"),
            clientAddress = extractField(text, "(?:Direcci[oó]n|DIRECCION)", "[^\\n]+"),
            clientCity = extractCity(text),
            issuerRuc = extractIssuerRuc(text),
            paymentTerms = extractField(text, "(?:Forma\\s*de\\s*pago|FORMA\\s*DE\\s*PAGO|Condici[oó]n)", "[^\\n]+"),
            deliveryDays = extractField(text, "(?:D[ií]as\\s*(?:de\\s*)?entrega|PLAZO)", "\\d+")?.toIntOrNull(),
            items = items,
            observations = extractObservations(text)
        )
    }

    private fun parseItems(text: String): List<ProformaItemRequest> {
        val items = mutableListOf<ProformaItemRequest>()
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        log.debug("Parsing ${lines.size} lines for items")

        // Strategy 1: Standard table format — Qty Description ... UnitPrice TotalPrice
        // Matches: "1 SERVICIO DE TRANSPORTE . 20.00 USD 20.00 USD 20.00"
        val pattern1 = Regex("^(\\d+)\\s+(.+?)\\s+\\.?\\s+[\\d,.]+\\s+USD\\s+([\\d,.]+)\\s+USD\\s+([\\d,.]+)")

        // Strategy 2: Qty Description Price Price (no USD labels)
        // Matches: "1 EQUIPO RD60S 300.00 300.00"
        val pattern2 = Regex("^(\\d+)\\s+(.{5,}?)\\s{2,}([\\d,.]+)\\s+([\\d,.]+)\\s*$")

        // Strategy 3: Qty | Description | UnitPrice | TotalPrice (tab/pipe separated)
        val pattern3 = Regex("^(\\d+)\\s*[|\\t]\\s*(.+?)\\s*[|\\t]\\s*([\\d,.]+)\\s*[|\\t]\\s*([\\d,.]+)")

        // Strategy 4: Lines with amounts at end — "DESCRIPCION LARGA 2 20.00 40.00"
        val pattern4 = Regex("^(.{5,}?)\\s+(\\d+)\\s+([\\d,.]+)\\s+([\\d,.]+)\\s*$")

        // Strategy 5: Amount pattern with $ signs — "1 Desc $20.00 $20.00"
        val pattern5 = Regex("^(\\d+)\\s+(.{3,}?)\\s+\\$\\s*([\\d,.]+)\\s+\\$\\s*([\\d,.]+)")

        // Skip header/footer keywords
        val skipKeywords = listOf(
            "subtotal", "total", "iva", "impuesto", "descuento",
            "observa", "nota:", "forma de pago", "vendedor", "atenci",
            "proforma", "fecha", "ruc", "direcci", "ciudad", "correo",
            "c[eé]dula", "tel[eé]fono", "p[aá]gina", "www\\.", "http",
            "banco", "cuenta", "transferencia"
        )
        val skipRegex = Regex(skipKeywords.joinToString("|"), RegexOption.IGNORE_CASE)

        var i = 0
        while (i < lines.size) {
            val line = lines[i]

            // Skip if line matches footer/header keywords
            if (skipRegex.containsMatchIn(line) && !line.matches(Regex("^\\d+\\s+.*"))) {
                i++; continue
            }

            var matched = false
            for (pattern in listOf(pattern1, pattern2, pattern3, pattern5)) {
                val match = pattern.find(line)
                if (match != null) {
                    val qty = match.groupValues[1].toIntOrNull() ?: 1
                    val desc = match.groupValues[2].trim()
                    val unitPrice = parseMoney(match.groupValues[3])
                    val totalPrice = parseMoney(match.groupValues[4])

                    if (desc.length >= 3 && totalPrice > BigDecimal.ZERO) {
                        var lot: String? = null
                        var expDate: LocalDate? = null
                        // Check next line for lot/expiry info
                        if (i + 1 < lines.size) {
                            val nextLine = lines[i + 1]
                            lot = Regex("(?:Lote|LOTE|LOT)[:\\s]*([\\w-]+)", RegexOption.IGNORE_CASE)
                                .find(nextLine)?.groupValues?.get(1)
                            val expStr = Regex("(?:Fecha\\s*(?:de\\s*)?(?:vcto|venc|expir)|Vence|EXP)[.:\\s]*([\\d/.-]+)", RegexOption.IGNORE_CASE)
                                .find(nextLine)?.groupValues?.get(1)
                            expDate = expStr?.let { parseDate(it) }
                            if (lot != null || expDate != null) i++
                        }

                        items.add(ProformaItemRequest(
                            quantity = qty, description = desc, lot = lot,
                            expirationDate = expDate, unitPrice = unitPrice, totalPrice = totalPrice
                        ))
                        matched = true
                        break
                    }
                }
            }

            // Strategy 4: description first, then qty and prices
            if (!matched) {
                val match4 = pattern4.find(line)
                if (match4 != null) {
                    val desc = match4.groupValues[1].trim()
                    val qty = match4.groupValues[2].toIntOrNull() ?: 1
                    val unitPrice = parseMoney(match4.groupValues[3])
                    val totalPrice = parseMoney(match4.groupValues[4])
                    if (desc.length >= 3 && totalPrice > BigDecimal.ZERO && !skipRegex.containsMatchIn(desc)) {
                        items.add(ProformaItemRequest(
                            quantity = qty, description = desc,
                            unitPrice = unitPrice, totalPrice = totalPrice
                        ))
                    }
                }
            }

            i++
        }

        // Deduplicate by description (some PDFs have repeated headers)
        return items.distinctBy { it.description.lowercase().trim() }
    }

    private fun extractProformaNumber(text: String): String? {
        // Try multiple patterns for proforma number
        val patterns = listOf(
            Regex("(?:Proforma|PROFORMA|Prof\\.?)\\s*#?:?\\s*(\\d{3,})", RegexOption.IGNORE_CASE),
            Regex("(?:N[°ºo]|No\\.|Nro\\.?)\\s*:?\\s*(\\d{3,})", RegexOption.IGNORE_CASE),
            Regex("(?:COTIZACI[OÓ]N|Cotización)\\s*#?:?\\s*(\\d{3,})", RegexOption.IGNORE_CASE)
        )
        for (p in patterns) {
            val match = p.find(text)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    private fun extractClientName(text: String): String? {
        val patterns = listOf(
            Regex("(?:ATENCI[OÓ]N\\s*A|Cliente|CLIENTE|Se[ñn]or(?:es|a)?)[:\\s]+([A-Za-zÀ-ÿ&.\\s]+)", RegexOption.IGNORE_CASE),
            Regex("(?:RAZON\\s*SOCIAL|Raz[oó]n\\s*Social)[:\\s]+([^\\n]+)", RegexOption.IGNORE_CASE)
        )
        for (p in patterns) {
            val match = p.find(text)
            if (match != null) return match.groupValues[1].trim().take(200)
        }
        return null
    }

    private fun extractIssuerRuc(text: String): String? {
        // First RUC in the document is typically the issuer
        val match = Regex("(?:R\\.?U\\.?C\\.?)[:\\s]*(\\d{10,13})").find(text)
        return match?.groupValues?.get(1)
    }

    private fun extractObservations(text: String): String? {
        val patterns = listOf(
            Regex("(?:Observaci[oó]n|OBSERVACI[OÓ]N|Nota|NOTA)[:\\s]+(.+?)(?=\\n\\n|$)", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
        )
        for (p in patterns) {
            val match = p.find(text)
            if (match != null) return match.groupValues[1].trim().take(1000)
        }
        return null
    }

    private fun extractMoney(text: String, labelPattern: String): BigDecimal? {
        val regex = Regex("$labelPattern\\s*([\\d,.]+)", RegexOption.IGNORE_CASE)
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.let { parseMoney(it) }
    }

    private fun parseMoney(value: String): BigDecimal {
        // Handle both "1,234.56" and "1.234,56" formats
        val cleaned = if (value.contains(',') && value.contains('.')) {
            if (value.lastIndexOf(',') > value.lastIndexOf('.')) {
                // European: 1.234,56
                value.replace(".", "").replace(",", ".")
            } else {
                // American: 1,234.56
                value.replace(",", "")
            }
        } else if (value.contains(',') && !value.contains('.')) {
            // Could be "1,234" or "1,56" — check decimal length
            val afterComma = value.substringAfterLast(',')
            if (afterComma.length <= 2) value.replace(",", ".")
            else value.replace(",", "")
        } else {
            value
        }
        return runCatching { BigDecimal(cleaned) }.getOrDefault(BigDecimal.ZERO)
    }

    private fun extractField(text: String, label: String, pattern: String): String? {
        val regex = Regex("$label[:\\s]+($pattern)", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.get(1)?.trim()
    }

    private fun extractDate(text: String, label: String): LocalDate? {
        val dateStr = extractField(text, label, "[\\d/.-]+") ?: return null
        return parseDate(dateStr)
    }

    private fun parseDate(dateStr: String): LocalDate? {
        for (fmt in dateFormats) {
            val date = runCatching { LocalDate.parse(dateStr, fmt) }.getOrNull()
            if (date != null) return date
        }
        return null
    }

    private fun extractCity(text: String): String? {
        val patterns = listOf(
            Regex("(?:Ciudad|CIUDAD)[:\\s]+([A-Za-zÀ-ÿ]+)", RegexOption.IGNORE_CASE),
            Regex("(?:Localidad|LOCALIDAD)[:\\s]+([A-Za-zÀ-ÿ]+)", RegexOption.IGNORE_CASE)
        )
        for (p in patterns) {
            val match = p.find(text)
            if (match != null) {
                val rawCity = match.groupValues[1].trim().uppercase()
                if (ecuadorianCities.contains(rawCity)) {
                    return rawCity
                }
            }
        }
        return null // Invalid city, let frontend handle it
    }
}
