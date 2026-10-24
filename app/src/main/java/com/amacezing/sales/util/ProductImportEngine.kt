package com.amacezing.sales.util

/**
 * Input rules for a product, shared by the product dialog and the product import so a value that is
 * valid in one is valid in the other. Messages are shown to the user as-is.
 */
object ProductInputRules {
    const val NAME_REQUIRED = "Product name cannot be empty."
    const val SELLING_PRICE_INVALID = "Please enter a valid selling price."

    fun nameError(name: String): String? = if (name.isBlank()) NAME_REQUIRED else null

    /** A selling price is valid when it is a number that is 0 or more; null otherwise. */
    fun validSellingPrice(value: Double?): Double? = value?.takeIf { it >= 0.0 }

    /** The dialog's cost rule: a blank or unreadable cost is stored as 0. */
    fun lenientUnitCost(text: String): Double = text.toDoubleOrNull() ?: 0.0
}

/** Columns of the product import file, in template order. The business is chosen on screen, not in the file. */
object ProductImportTemplate {
    val HEADERS = listOf("Product Name", "Selling Price", "Unit Cost", "Active")
    val REQUIRED = listOf("Product Name", "Selling Price")

    fun csv(): String = HEADERS.joinToString(",") + "\r\n"
    fun xlsx(): ByteArray = SimpleXlsx.write(listOf(HEADERS), sheetName = "Products")
}

/** One valid row of a product import. [existingId] is set when the business already has a product with this name. */
data class ProductImportRow(
    val fileRow: Int,
    val name: String,
    val sellingPrice: Double,
    val unitCost: Double,
    val isActive: Boolean,
    val existingId: Long?
)

data class ProductImportPlan(
    val rowCount: Int,
    val rows: List<ProductImportRow>,
    val issues: List<ImportIssue>,
    /** Set when the file can't be used at all (e.g. required columns are missing). */
    val fatalProblem: String?
) {
    val newCount: Int get() = rows.count { it.existingId == null }
    val existingCount: Int get() = rows.count { it.existingId != null }
    val invalidRowCount: Int get() = issues.map { it.row }.distinct().size

    /** Rows that will be saved: new products, plus existing ones only when [updateExisting] is on. */
    fun rowsToSave(updateExisting: Boolean): List<ProductImportRow> =
        rows.filter { it.existingId == null || updateExisting }
}

object ProductImportEngine {
    private fun norm(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

    private val ALIASES: Map<String, List<String>> = mapOf(
        "name" to listOf("productname", "product", "name", "item", "itemname", "service"),
        "price" to listOf("sellingprice", "price", "unitprice"),
        "cost" to listOf("unitcost", "cost"),
        "active" to listOf("active", "isactive", "activeincatalog")
    )

    private val YES = setOf("yes", "y", "true", "1", "active")
    private val NO = setOf("no", "n", "false", "0", "inactive")

    /**
     * Checks every row against the same rules as the product dialog (name required, selling price a
     * number of 0 or more, cost optional). Unlike the dialog, an unreadable or negative Unit Cost is
     * reported instead of silently becoming 0, because a bulk file can hide such typos.
     */
    fun validate(rows: List<List<String>>, businessId: String, existing: List<ProductRef>): ProductImportPlan {
        if (rows.isEmpty()) return ProductImportPlan(0, emptyList(), emptyList(), "The file has no rows.")
        val header = rows.first().map { norm(it) }
        val col = mutableMapOf<String, Int>()
        ALIASES.forEach { (key, names) ->
            val idx = header.indexOfFirst { it in names }
            if (idx >= 0) col[key] = idx
        }
        val missing = buildList {
            if ("name" !in col) add("Product Name")
            if ("price" !in col) add("Selling Price")
        }
        if (missing.isNotEmpty()) {
            return ProductImportPlan(
                rows.size - 1, emptyList(), emptyList(),
                "Missing required column${if (missing.size > 1) "s" else ""}: ${missing.joinToString(", ")}. " +
                    "Download the template to see the expected header row."
            )
        }
        if (rows.size == 1) return ProductImportPlan(0, emptyList(), emptyList(), "The file has a header but no data rows.")

        fun cell(row: List<String>, key: String): String = col[key]?.let { row.getOrNull(it) }?.trim().orEmpty()

        val catalog = existing.filter { it.businessId == businessId }
            .groupBy { it.name.trim().lowercase() }
            .mapValues { (_, list) -> list.minOf { it.id } }

        val issues = mutableListOf<ImportIssue>()
        val valid = mutableListOf<ProductImportRow>()
        val firstSeen = HashMap<String, Int>()

        for (i in 1 until rows.size) {
            val row = rows[i]
            val fileRow = i + 1
            if (row.all { it.isBlank() }) continue
            var ok = true
            fun fail(msg: String) { issues.add(ImportIssue(fileRow, msg)); ok = false }

            val name = cell(row, "name")
            ProductInputRules.nameError(name)?.let { fail(it) }

            val price = ProductInputRules.validSellingPrice(ImportEngine.parseNumber(cell(row, "price")))
            if (price == null) fail(ProductInputRules.SELLING_PRICE_INVALID)

            val costText = cell(row, "cost")
            val cost = if (costText.isEmpty()) 0.0 else ImportEngine.parseNumber(costText)?.takeIf { it >= 0 }
                .also { if (it == null) fail("Unit Cost must be a number (0 or more), or left blank.") }

            val activeText = cell(row, "active").lowercase()
            val active = when {
                activeText.isEmpty() -> true
                activeText in YES -> true
                activeText in NO -> false
                else -> { fail("Active must be Yes or No, or left blank."); true }
            }

            val key = name.lowercase()
            if (name.isNotEmpty()) {
                val first = firstSeen[key]
                if (first != null) fail("\"$name\" is repeated (first on row $first).") else firstSeen[key] = fileRow
            }
            if (!ok) continue
            valid.add(ProductImportRow(fileRow, name, price!!, cost!!, active, catalog[key]))
        }
        return ProductImportPlan(rows.size - 1, valid, issues.sortedBy { it.row }, null)
    }
}
