package com.amacezing.sales.util

import com.amacezing.sales.model.Business
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

/** Columns of the order import file, in template order. */
object ImportTemplate {
    val HEADERS = listOf(
        "Order Ref", "Order Date", "Customer", "Business", "Product", "Quantity",
        "Unit Price", "Unit Cost", "Discount %", "Amount Paid", "Payment Method", "Remarks"
    )
    val REQUIRED = listOf("Order Date", "Customer", "Business", "Product", "Quantity", "Unit Price")

    fun csv(): String = HEADERS.joinToString(",") + "\r\n"
    fun xlsx(): ByteArray = SimpleXlsx.write(listOf(HEADERS), sheetName = "Orders")
}

/** Reads CSV or XLSX bytes into rows of text cells. Throws IllegalArgumentException with a readable message. */
object TabularReader {
    const val MAX_BYTES = 10 * 1024 * 1024
    const val MAX_ROWS = 5000

    fun read(fileName: String, bytes: ByteArray): List<List<String>> {
        require(bytes.isNotEmpty()) { "The file is empty." }
        require(bytes.size <= MAX_BYTES) { "The file is larger than 10 MB." }
        val isZip = bytes.size > 3 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()
        val lower = fileName.lowercase()
        val rows = when {
            isZip || lower.endsWith(".xlsx") -> {
                require(isZip) { "This doesn't look like a valid .xlsx file." }
                SimpleXlsx.read(bytes)
            }
            lower.endsWith(".xls") -> throw IllegalArgumentException(
                "Old .xls files aren't supported. Save the sheet as .xlsx or .csv and try again."
            )
            else -> parseCsv(String(bytes, Charsets.UTF_8))
        }
        require(rows.size <= MAX_ROWS + 1) { "Too many rows (limit is $MAX_ROWS orders lines per file)." }
        return rows
    }

    fun parseCsv(raw: String): List<List<String>> {
        val text = raw.removePrefix("\uFEFF")
        val firstLine = text.lineSequence().firstOrNull().orEmpty()
        val delimiter = listOf(',', ';', '\t').maxByOrNull { d -> firstLine.count { it == d } }
            ?.takeIf { firstLine.contains(it) } ?: ','

        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        fun endCell() { row.add(cell.toString()); cell.setLength(0) }
        fun endRow() {
            endCell()
            if (row.any { it.isNotBlank() }) rows.add(row)
            row = mutableListOf()
        }
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> { cell.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && c == delimiter -> endCell()
                !inQuotes && c == '\n' -> endRow()
                !inQuotes && c == '\r' -> { /* handled with \n */ }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows
    }
}

/** Minimal .xlsx reader/writer (first sheet, text and numbers only) using only the JDK. */
object SimpleXlsx {
    private const val MAX_ENTRY_BYTES = 20 * 1024 * 1024

    fun read(bytes: ByteArray): List<List<String>> {
        var sharedXml: ByteArray? = null
        val sheets = sortedMapOf<Int, ByteArray>()
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    val wanted = name == "xl/sharedStrings.xml" ||
                        (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml"))
                    if (wanted) {
                        val data = readLimited(zip)
                        if (name == "xl/sharedStrings.xml") sharedXml = data
                        else sheets[name.removePrefix("xl/worksheets/sheet").removeSuffix(".xml").toIntOrNull() ?: 0] = data
                    }
                    entry = zip.nextEntry
                }
            }
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("Couldn't open the Excel file. Make sure it's a valid .xlsx.")
        }
        val sheet = sheets.values.firstOrNull() ?: throw IllegalArgumentException("No worksheet found in the Excel file.")
        val shared = sharedXml?.let { parseSharedStrings(it) } ?: emptyList()
        return parseSheet(sheet, shared)
    }

    private fun readLimited(zip: ZipInputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (true) {
            val n = zip.read(buf)
            if (n < 0) break
            total += n
            require(total <= MAX_ENTRY_BYTES) { "The Excel file is too large to read." }
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    private fun parser() = SAXParserFactory.newInstance().apply {
        isNamespaceAware = false
        // Harden against XML entity tricks; not every platform parser supports every feature.
        listOf(
            "http://apache.org/xml/features/disallow-doctype-decl" to true,
            "http://xml.org/sax/features/external-general-entities" to false,
            "http://xml.org/sax/features/external-parameter-entities" to false
        ).forEach { (f, v) -> try { setFeature(f, v) } catch (_: Exception) {} }
    }.newSAXParser()

    private fun parseSharedStrings(xml: ByteArray): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inT = false
        var inPhonetic = false
        parser().parse(ByteArrayInputStream(xml), object : DefaultHandler() {
            override fun startElement(u: String?, l: String?, q: String, a: Attributes?) {
                when (q) {
                    "si" -> sb.setLength(0)
                    "t" -> inT = true
                    "rPh" -> inPhonetic = true
                }
            }
            override fun endElement(u: String?, l: String?, q: String) {
                when (q) {
                    "si" -> result.add(sb.toString())
                    "t" -> inT = false
                    "rPh" -> inPhonetic = false
                }
            }
            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (inT && !inPhonetic) sb.append(ch, start, length)
            }
        })
        return result
    }

    private fun columnIndex(ref: String): Int {
        var n = 0
        for (c in ref) {
            if (c in 'A'..'Z') n = n * 26 + (c - 'A' + 1) else break
        }
        return (n - 1).coerceAtLeast(0)
    }

    private fun parseSheet(xml: ByteArray, shared: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        var col = 0
        var type = ""
        val text = StringBuilder()
        var inValue = false
        var inInline = false
        parser().parse(ByteArrayInputStream(xml), object : DefaultHandler() {
            override fun startElement(u: String?, l: String?, q: String, a: Attributes?) {
                when (q) {
                    "row" -> current = mutableListOf()
                    "c" -> {
                        col = columnIndex(a?.getValue("r").orEmpty())
                        type = a?.getValue("t").orEmpty()
                        text.setLength(0)
                    }
                    "v" -> inValue = true
                    "is" -> inInline = true
                    "t" -> if (inInline) inValue = true
                }
            }
            override fun endElement(u: String?, l: String?, q: String) {
                when (q) {
                    "v" -> inValue = false
                    "t" -> if (inInline) inValue = false
                    "is" -> inInline = false
                    "c" -> {
                        val raw = text.toString()
                        val value = when (type) {
                            "s" -> raw.trim().toIntOrNull()?.let { shared.getOrNull(it) } ?: ""
                            else -> raw
                        }
                        while (current.size < col) current.add("")
                        if (current.size == col) current.add(value) else current[col] = value
                    }
                    "row" -> if (current.any { it.isNotBlank() }) rows.add(current)
                }
            }
            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (inValue) text.append(ch, start, length)
            }
        })
        return rows
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun colName(i: Int): String {
        var n = i + 1
        val sb = StringBuilder()
        while (n > 0) { val r = (n - 1) % 26; sb.insert(0, 'A' + r); n = (n - 1) / 26 }
        return sb.toString()
    }

    fun write(rows: List<List<String>>, sheetName: String = "Sheet1"): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun put(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name)); zip.write(content.toByteArray(Charsets.UTF_8)); zip.closeEntry()
            }
            val xmlHead = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            put("[Content_Types].xml", xmlHead +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                "</Types>")
            put("_rels/.rels", xmlHead +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                "</Relationships>")
            put("xl/workbook.xml", xmlHead +
                "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                "<sheets><sheet name=\"${esc(sheetName)}\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>")
            put("xl/_rels/workbook.xml.rels", xmlHead +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
                "</Relationships>")
            val width = rows.maxOfOrNull { it.size } ?: 1
            val sb = StringBuilder(xmlHead)
            sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
            sb.append("<cols><col min=\"1\" max=\"$width\" width=\"18\" customWidth=\"1\"/></cols><sheetData>")
            rows.forEachIndexed { r, row ->
                sb.append("<row r=\"${r + 1}\">")
                row.forEachIndexed { c, v ->
                    sb.append("<c r=\"${colName(c)}${r + 1}\" t=\"inlineStr\"><is><t>${esc(v)}</t></is></c>")
                }
                sb.append("</row>")
            }
            sb.append("</sheetData></worksheet>")
            put("xl/worksheets/sheet1.xml", sb.toString())
        }
        return out.toByteArray()
    }
}

data class ImportIssue(val row: Int, val message: String)

data class ImportLine(
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    /** Null when the file gave no cost; the catalog cost is used then. */
    val unitCost: Double?,
    val productId: Long?
)

data class ImportOrder(
    val firstRow: Int,
    val dateMillis: Long,
    val customer: String,
    val businessId: String,
    val businessName: String,
    val discountPercent: Double,
    val amountPaid: Double,
    val paymentMethod: String,
    val remarks: String,
    val lines: List<ImportLine>,
    val gross: Double,
    val net: Double,
    val isDuplicate: Boolean
)

data class ImportPlan(
    val rowCount: Int,
    val orders: List<ImportOrder>,
    val issues: List<ImportIssue>,
    /** Set when the file can't be used at all (e.g. required columns are missing). */
    val fatalProblem: String?
) {
    val invalidOrderCount: Int get() = issues.map { it.row }.distinct().size
    val duplicateCount: Int get() = orders.count { it.isDuplicate }
}

/** What the validator needs to know about the app's data; kept as plain values so it is easy to test. */
data class ImportContext(
    val businesses: List<Business>,
    val products: List<ProductRef>,
    val existingOrders: List<ExistingOrderRef>
)
data class ProductRef(val id: Long, val businessId: String, val name: String, val unitCost: Double)
data class ExistingOrderRef(val businessId: String, val customer: String, val dateMillis: Long, val netSales: Double)

object ImportEngine {
    private val ZONE = Formatters.MANILA_ZONE_ID

    private fun norm(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

    private val ALIASES: Map<String, List<String>> = mapOf(
        "ref" to listOf("orderref", "ref", "orderreference", "reference", "orderno", "ordernumber"),
        "date" to listOf("orderdate", "date"),
        "customer" to listOf("customer", "customername", "client"),
        "business" to listOf("business", "businessname", "division"),
        "product" to listOf("product", "productname", "item", "service"),
        "qty" to listOf("quantity", "qty"),
        "price" to listOf("unitprice", "price", "sellingprice"),
        "cost" to listOf("unitcost", "cost"),
        "discount" to listOf("discount", "discountpercent", "discountpct"),
        "paid" to listOf("amountpaid", "paid", "payment"),
        "method" to listOf("paymentmethod", "method"),
        "remarks" to listOf("remarks", "notes", "note")
    )
    private val REQUIRED_KEYS = listOf("date" to "Order Date", "customer" to "Customer", "business" to "Business",
        "product" to "Product", "qty" to "Quantity", "price" to "Unit Price")

    fun parseNumber(raw: String): Double? {
        val cleaned = raw.trim().replace("₱", "").replace("PHP", "", ignoreCase = true)
            .replace(",", "").replace(" ", "")
        if (cleaned.isEmpty()) return null
        return cleaned.toDoubleOrNull()?.takeIf { !it.isNaN() && !it.isInfinite() }
    }

    private val DATE_FORMATS = listOf(
        "yyyy-MM-dd", "yyyy/MM/dd", "d/M/yyyy", "d-M-yyyy", "d MMM yyyy", "d-MMM-yyyy", "MMM d, yyyy", "d MMMM yyyy"
    ).map { DateTimeFormatter.ofPattern(it, Locale.ENGLISH) }

    /** Accepts ISO dates, day/month/year, month names, and Excel date serial numbers. */
    fun parseDate(raw: String): Long? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        text.toDoubleOrNull()?.let { serial ->
            if (serial in 20000.0..80000.0) {
                return LocalDate.of(1899, 12, 30).plusDays(serial.toLong()).atStartOfDay(ZONE).toInstant().toEpochMilli()
            }
        }
        for (f in DATE_FORMATS) {
            try { return LocalDate.parse(text, f).atStartOfDay(ZONE).toInstant().toEpochMilli() } catch (_: Exception) {}
        }
        return null
    }

    private fun localDate(millis: Long) = Instant.ofEpochMilli(millis).atZone(ZONE).toLocalDate()

    fun validate(rows: List<List<String>>, ctx: ImportContext): ImportPlan {
        if (rows.isEmpty()) return ImportPlan(0, emptyList(), emptyList(), "The file has no rows.")
        val header = rows.first().map { norm(it) }
        val col = mutableMapOf<String, Int>()
        ALIASES.forEach { (key, names) ->
            val idx = header.indexOfFirst { it in names }
            if (idx >= 0) col[key] = idx
        }
        val missing = REQUIRED_KEYS.filter { it.first !in col }.map { it.second }
        if (missing.isNotEmpty()) {
            return ImportPlan(
                rows.size - 1, emptyList(), emptyList(),
                "Missing required column${if (missing.size > 1) "s" else ""}: ${missing.joinToString(", ")}. " +
                    "Download the template to see the expected header row."
            )
        }
        if (rows.size == 1) return ImportPlan(0, emptyList(), emptyList(), "The file has a header but no data rows.")

        fun cell(row: List<String>, key: String): String = col[key]?.let { row.getOrNull(it) }?.trim().orEmpty()

        data class Parsed(
            val fileRow: Int, val ref: String, val date: Long?, val customer: String, val business: Business?,
            val line: ImportLine?, val discount: Double?, val paid: Double?, val method: String, val remarks: String
        )

        val issues = mutableListOf<ImportIssue>()
        val parsed = mutableListOf<Parsed>()
        val byName = ctx.businesses.associateBy { it.name.trim().lowercase() }
        val byId = ctx.businesses.associateBy { it.id.lowercase() }

        for (i in 1 until rows.size) {
            val row = rows[i]
            val fileRow = i + 1
            if (row.all { it.isBlank() }) continue
            var ok = true
            fun fail(msg: String) { issues.add(ImportIssue(fileRow, msg)); ok = false }

            val date = parseDate(cell(row, "date"))
            if (date == null) fail("Invalid or missing Order Date \"${cell(row, "date")}\" (use yyyy-MM-dd, e.g. 2026-10-23).")
            val customer = cell(row, "customer")
            if (customer.isEmpty()) fail("Customer is required.")
            val businessText = cell(row, "business")
            val business = byName[businessText.lowercase()] ?: byId[businessText.lowercase()]
            if (business == null) fail("Business \"$businessText\" doesn't exist. Add it in Settings first, or fix the spelling.")
            else if (!business.isActive) fail("Business \"${business.name}\" is archived.")
            val product = cell(row, "product")
            if (product.isEmpty()) fail("Product is required.")
            val qtyD = parseNumber(cell(row, "qty"))
            val qty = qtyD?.takeIf { it > 0 && it == Math.floor(it) && it <= 1_000_000 }?.toInt()
            if (qty == null) fail("Quantity must be a whole number greater than 0.")
            val price = parseNumber(cell(row, "price"))?.takeIf { it >= 0 }
            if (price == null) fail("Unit Price must be a number (0 or more).")
            val costText = cell(row, "cost")
            val cost = if (costText.isEmpty()) null else parseNumber(costText)?.takeIf { it >= 0 }
                .also { if (it == null) fail("Unit Cost must be a number (0 or more), or left blank.") }
            val discText = cell(row, "discount")
            val discount = if (discText.isEmpty()) 0.0 else parseNumber(discText.removeSuffix("%"))?.takeIf { it in 0.0..100.0 }
                .also { if (it == null) fail("Discount % must be between 0 and 100.") }
            val paidText = cell(row, "paid")
            val paid = if (paidText.isEmpty()) 0.0 else parseNumber(paidText)?.takeIf { it >= 0 }
                .also { if (it == null) fail("Amount Paid must be a number (0 or more).") }

            if (!ok) { parsed.add(Parsed(fileRow, cell(row, "ref"), null, customer, business, null, null, null, "", "")); continue }

            val match = ctx.products.firstOrNull { it.businessId == business!!.id && it.name.equals(product, ignoreCase = true) }
            parsed.add(
                Parsed(
                    fileRow, cell(row, "ref"), date, customer, business,
                    ImportLine(product, qty!!, price!!, cost, match?.id),
                    discount, paid, cell(row, "method").ifEmpty { "Cash" }, cell(row, "remarks")
                )
            )
        }

        // Group lines: same Order Ref = one order; blank ref = one order per row.
        val groups = LinkedHashMap<String, MutableList<Parsed>>()
        parsed.forEach { p ->
            val key = if (p.ref.isBlank()) "row#${p.fileRow}" else "ref#${p.ref.lowercase()}"
            groups.getOrPut(key) { mutableListOf() }.add(p)
        }

        val orders = mutableListOf<ImportOrder>()
        for ((_, g) in groups) {
            val first = g.first()
            var groupOk = g.all { it.line != null }
            if (groupOk) {
                for (other in g.drop(1)) {
                    val sameHeader = other.customer.equals(first.customer, true) &&
                        other.business?.id == first.business?.id &&
                        other.date?.let { localDate(it) } == first.date?.let { localDate(it) }
                    if (!sameHeader) {
                        issues.add(ImportIssue(other.fileRow, "Rows with the same Order Ref must share Customer, Business and Order Date (row ${first.fileRow} differs)."))
                        groupOk = false
                    }
                }
            }
            if (!groupOk) continue
            val gross = g.sumOf { it.line!!.quantity * it.line.unitPrice }
            val pct = first.discount ?: 0.0
            val net = SalesCalculator.roundTo2Decimals(gross - gross * pct / 100.0)
            val paid = first.paid ?: 0.0
            if (paid > net + 0.01) {
                issues.add(ImportIssue(first.fileRow, "Amount Paid (${Formatters.formatCurrency(paid)}) is more than the order total (${Formatters.formatCurrency(net)})."))
                continue
            }
            val business = first.business!!
            val day = localDate(first.date!!)
            val duplicate = ctx.existingOrders.any {
                it.businessId == business.id && it.customer.equals(first.customer, true) &&
                    localDate(it.dateMillis) == day && Math.abs(it.netSales - net) < 0.01
            }
            orders.add(
                ImportOrder(
                    first.fileRow, first.date, first.customer, business.id, business.name, pct, paid,
                    first.method, g.firstOrNull { it.remarks.isNotBlank() }?.remarks.orEmpty(),
                    g.map { it.line!! }, SalesCalculator.roundTo2Decimals(gross), net, duplicate
                )
            )
        }
        return ImportPlan(rows.size - 1, orders, issues.sortedBy { it.row }, null)
    }
}
