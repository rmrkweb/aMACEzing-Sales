package com.amacezing.sales.util

import com.amacezing.sales.model.Business
import com.amacezing.sales.model.TransactionWithDetails

/** CSV export of existing orders. (Importing lives in [ImportEngine].) */
object CsvHelper {

    fun exportTransactionsToCsv(
        transactions: List<TransactionWithDetails>,
        businessNames: Map<String, String> = emptyMap()
    ): String {
        val sb = StringBuilder()
        sb.append("Order Number,Business,Date,Customer,Line Items,Gross Sales,Discount (%),Net Sales,Estimated Profit,Amount Paid,Balance,Payment Status,Remarks\n")
        for (item in transactions) {
            val tx = item.transaction
            val itemsSummary = item.lineItems.joinToString(";") { "${it.productName} (x${it.quantity})" }
            val dateStr = Formatters.formatEpochMillisToDate(tx.orderDateEpochMillis)
            sb.append("\"${escapeCsv(tx.orderNumber)}\",")
            sb.append("\"${escapeCsv(businessNames[tx.businessType] ?: Business.fallbackName(tx.businessType))}\",")
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
}
