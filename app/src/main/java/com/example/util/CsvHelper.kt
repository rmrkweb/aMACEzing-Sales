package com.example.util

import com.example.model.BusinessType
import com.example.model.DiscountType
import com.example.model.PaymentStatus
import com.example.model.SalesTransaction
import com.example.model.TransactionLineItem
import com.example.model.TransactionWithDetails
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

data class ImportValidationRow(
    val rowNumber: Int,
    val sheetName: String,
    val businessType: BusinessType,
    val rawCustomer: String,
    val rawDate: String,
    val rawProduct1Qty: String,
    val rawProduct2Qty: String,
    val rawDiscount: String,
    val rawTotal: String,
    val rawProfit: String,
    val rawRemarks: String,
    val isValid: Boolean,
    val isDuplicate: Boolean,
    val validationErrors: List<String>,
    val parsedDateMillis: Long = 0L,
    val product1Qty: Int = 0,
    val product2Qty: Int = 0,
    val discountPercent: Double = 0.0,
    val calculatedGross: Double = 0.0,
    val calculatedNet: Double = 0.0,
    val calculatedProfit: Double = 0.0,
    val excelTotalDiff: Double = 0.0,
    val excelProfitDiff: Double = 0.0
)

data class ImportPreviewSummary(
    val totalDetected: Int,
    val validCount: Int,
    val invalidCount: Int,
    val duplicateCount: Int,
    val rows: List<ImportValidationRow>,
    val errorReport: List<String>
)

object CsvHelper {

    fun exportTransactionsToCsv(transactions: List<TransactionWithDetails>): String {
        val sb = StringBuilder()
        sb.append("Order Number,Business,Date,Customer,Line Items,Gross Sales,Discount (%),Net Sales,Estimated Profit,Amount Paid,Balance,Payment Status,Remarks\n")
        for (item in transactions) {
            val tx = item.transaction
            val itemsSummary = item.lineItems.joinToString(";") { "${it.productName} (x${it.quantity})" }
            val dateStr = Formatters.formatEpochMillisToDate(tx.orderDateEpochMillis)
            sb.append("\"${escapeCsv(tx.orderNumber)}\",")
            sb.append("\"${escapeCsv(tx.businessType.displayName)}\",")
            sb.append("\"${escapeCsv(dateStr)}\",")
            sb.append("\"${escapeCsv(tx.customerName)}\",")
            sb.append("\"${escapeCsv(itemsSummary)}\",")
            sb.append("${tx.grossSales},")
            sb.append("${tx.discountPercentage},")
            sb.append("${tx.netSales},")
            sb.append("${tx.estimatedProfit},")
            sb.append("${tx.amountPaid},")
            sb.append("${tx.outstandingBalance},")
            sb.append("\"${escapeCsv(tx.paymentStatus.displayName)}\",")
            sb.append("\"${escapeCsv(tx.remarks)}\"\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }

    fun parseDateFlexibly(dateStr: String): Long? {
        val trimmed = dateStr.trim()
        val formatters = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US),
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US),
            DateTimeFormatter.ofPattern("d/M/yyyy", Locale.US),
            DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.US),
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US),
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US),
            DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US),
            DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US)
        )
        for (f in formatters) {
            try {
                val ld = LocalDate.parse(trimmed, f)
                return ld.atStartOfDay(Formatters.MANILA_ZONE_ID).toInstant().toEpochMilli()
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun parseAndValidateExcelCsv(
        content: String,
        businessType: BusinessType,
        sheetName: String,
        existingTransactions: List<TransactionWithDetails>,
        sintraMarginPercent: Double = 58.3333
    ): ImportPreviewSummary {
        val lines = content.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (lines.isEmpty()) {
            return ImportPreviewSummary(0, 0, 0, 0, emptyList(), listOf("No content provided for import."))
        }

        // Determine if first line is header
        val startIndex = if (lines[0].lowercase().contains("customer") || lines[0].lowercase().contains("date")) 1 else 0

        val rows = mutableListOf<ImportValidationRow>()
        val errorReport = mutableListOf<String>()

        // Product configurations for parsing
        // Engraving: Coaster (₱70/₱35), Ballpen (₱40/₱20)
        // Sintra: A4 (₱150), A3 (₱300)
        val p1Selling = if (businessType == BusinessType.ENGRAVING) 70.0 else 150.0
        val p1Cost = if (businessType == BusinessType.ENGRAVING) 35.0 else 62.50
        val p2Selling = if (businessType == BusinessType.ENGRAVING) 40.0 else 300.0
        val p2Cost = if (businessType == BusinessType.ENGRAVING) 20.0 else 125.00

        for (i in startIndex until lines.size) {
            val line = lines[i]
            // Support comma, tab, or pipe separated
            val cols = splitCsvLine(line)
            if (cols.isEmpty() || cols.all { it.isBlank() }) continue

            val rowNum = i + 1
            val rawCustomer = cols.getOrElse(0) { "" }.trim()
            val rawDate = cols.getOrElse(1) { "" }.trim()
            val rawP1Qty = cols.getOrElse(2) { "0" }.trim()
            val rawP2Qty = cols.getOrElse(3) { "0" }.trim()
            val rawDiscount = cols.getOrElse(4) { "0" }.trim().replace("%", "")
            val rawTotal = cols.getOrElse(5) { "0" }.trim().replace("₱", "").replace(",", "")
            val rawProfit = cols.getOrElse(6) { "0" }.trim().replace("₱", "").replace(",", "")
            val rawRemarks = cols.getOrElse(7) { "" }.trim()

            val rowErrors = mutableListOf<String>()

            if (rawCustomer.isBlank()) {
                rowErrors.add("Missing Customer Name")
            }

            val parsedDateMillis = parseDateFlexibly(rawDate)
            if (parsedDateMillis == null) {
                rowErrors.add("Invalid Date: '$rawDate' (Expected DD/MM/YYYY or YYYY-MM-DD)")
            }

            val p1Qty = rawP1Qty.toIntOrNull() ?: 0
            val p2Qty = rawP2Qty.toIntOrNull() ?: 0
            if (p1Qty <= 0 && p2Qty <= 0) {
                rowErrors.add("Order must have at least 1 product quantity")
            }

            val discountVal = rawDiscount.toDoubleOrNull() ?: 0.0
            val excelTotal = rawTotal.toDoubleOrNull() ?: 0.0
            val excelProfit = rawProfit.toDoubleOrNull() ?: 0.0

            val calcResult = SalesCalculator.calculate(
                businessType = businessType,
                items = listOf(
                    LineItemInput(p1Qty, p1Selling, p1Cost),
                    LineItemInput(p2Qty, p2Selling, p2Cost)
                ),
                discountType = DiscountType.PERCENTAGE,
                discountValue = discountVal,
                sintraMarginPercent = sintraMarginPercent
            )

            val totalDiff = if (excelTotal > 0.0) SalesCalculator.roundTo2Decimals(calcResult.netSales - excelTotal) else 0.0
            val profitDiff = if (excelProfit > 0.0) SalesCalculator.roundTo2Decimals(calcResult.estimatedProfit - excelProfit) else 0.0

            // Check duplicate candidate against existing transactions
            val isDuplicate = existingTransactions.any { existing ->
                existing.transaction.businessType == businessType &&
                existing.transaction.customerName.equals(rawCustomer, ignoreCase = true) &&
                (parsedDateMillis != null && Math.abs(existing.transaction.orderDateEpochMillis - parsedDateMillis) < 86400000L) &&
                Math.abs(existing.transaction.netSales - calcResult.netSales) < 0.1
            }

            val isValid = rowErrors.isEmpty()
            if (!isValid) {
                errorReport.add("Row $rowNum ($sheetName): ${rowErrors.joinToString(", ")}")
            }

            rows.add(
                ImportValidationRow(
                    rowNumber = rowNum,
                    sheetName = sheetName,
                    businessType = businessType,
                    rawCustomer = rawCustomer,
                    rawDate = rawDate,
                    rawProduct1Qty = rawP1Qty,
                    rawProduct2Qty = rawP2Qty,
                    rawDiscount = rawDiscount,
                    rawTotal = rawTotal,
                    rawProfit = rawProfit,
                    rawRemarks = rawRemarks,
                    isValid = isValid,
                    isDuplicate = isDuplicate,
                    validationErrors = rowErrors,
                    parsedDateMillis = parsedDateMillis ?: System.currentTimeMillis(),
                    product1Qty = p1Qty,
                    product2Qty = p2Qty,
                    discountPercent = discountVal,
                    calculatedGross = calcResult.grossSales,
                    calculatedNet = calcResult.netSales,
                    calculatedProfit = calcResult.estimatedProfit,
                    excelTotalDiff = totalDiff,
                    excelProfitDiff = profitDiff
                )
            )
        }

        val totalDetected = rows.size
        val validCount = rows.count { it.isValid }
        val invalidCount = rows.count { !it.isValid }
        val duplicateCount = rows.count { it.isDuplicate }

        return ImportPreviewSummary(
            totalDetected = totalDetected,
            validCount = validCount,
            invalidCount = invalidCount,
            duplicateCount = duplicateCount,
            rows = rows,
            errorReport = errorReport
        )
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        val delimiter = if (line.contains("\t")) '\t' else ','

        for (ch in line) {
            when {
                ch == '\"' -> inQuotes = !inQuotes
                ch == delimiter && !inQuotes -> {
                    result.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(ch)
            }
        }
        result.add(sb.toString().trim())
        return result
    }
}
