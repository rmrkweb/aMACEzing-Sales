package com.amacezing.sales.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amacezing.sales.model.BusinessType
import com.amacezing.sales.model.PaymentStatus
import com.amacezing.sales.ui.components.BusinessFilterChips
import com.amacezing.sales.ui.theme.EngravingBrown
import com.amacezing.sales.ui.theme.SecondaryAmber
import com.amacezing.sales.ui.theme.SintraBoardBlue
import com.amacezing.sales.ui.theme.StatusPaid
import com.amacezing.sales.ui.theme.StatusPending
import com.amacezing.sales.util.Formatters
import com.amacezing.sales.viewmodel.DateRangeType
import com.amacezing.sales.viewmodel.SalesViewModel

@Composable
fun ReportsScreen(
    viewModel: SalesViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedBusiness by viewModel.selectedBusiness.collectAsState()
    val dateRangeType by viewModel.dateRangeType.collectAsState()
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val productStats by viewModel.productSalesStats.collectAsState()
    val businessComparison by viewModel.businessComparison.collectAsState()

    // Customer sales summary in this period
    val customerSalesSummary = remember(filteredTransactions) {
        val active = filteredTransactions.filter { it.transaction.paymentStatus != PaymentStatus.CANCELLED }
        active.groupBy { it.transaction.customerName }
            .map { (custName, txs) ->
                val net = txs.sumOf { it.transaction.netSales }
                val paid = txs.sumOf { it.transaction.amountPaid }
                val bal = txs.sumOf { it.transaction.outstandingBalance }
                val orders = txs.size
                Triple(custName, Pair(net, paid), Pair(bal, orders))
            }
            .sortedByDescending { it.second.first }
    }

    fun exportReportCsv() {
        val sb = StringBuilder()
        sb.append("A-MACEZING SALES REPORT\n")
        sb.append("Period:,${dateRangeType.label}\n")
        sb.append("Business Filter:,${selectedBusiness?.displayName ?: "All Businesses"}\n")
        sb.append("Generated On:,${Formatters.formatEpochMillisToDate(System.currentTimeMillis())} (Asia/Manila)\n\n")

        sb.append("FINANCIAL EXECUTIVE SUMMARY\n")
        sb.append("Metric,Amount (PHP)\n")
        sb.append("Gross Sales,${metrics.grossSales}\n")
        sb.append("Total Discounts,${metrics.totalDiscounts}\n")
        sb.append("Net Sales,${metrics.netSales}\n")
        sb.append("Estimated Profit,${metrics.estimatedProfit}\n")
        sb.append("Profit Margin (%),${metrics.profitMarginPercent}%\n")
        sb.append("Total Amount Collected,${metrics.totalCollected}\n")
        sb.append("Outstanding Receivables,${metrics.outstandingReceivables}\n")
        sb.append("Total Orders,${metrics.ordersCount}\n")
        sb.append("Total Products Sold,${metrics.totalProductsSold}\n\n")

        sb.append("PRODUCT SALES BREAKDOWN\n")
        sb.append("Product Name,Business,Units Sold,Net Sales (PHP)\n")
        for (p in productStats) {
            sb.append("\"${p.productName}\",\"${p.businessType.displayName}\",${p.totalQuantity},${p.totalSales}\n")
        }
        sb.append("\n")

        sb.append("CUSTOMER SALES BREAKDOWN\n")
        sb.append("Customer Name,Orders,Net Sales (PHP),Collected (PHP),Balance (PHP)\n")
        for (c in customerSalesSummary) {
            sb.append("\"${c.first}\",${c.third.second},${c.second.first},${c.second.second},${c.third.first}\n")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            putExtra(Intent.EXTRA_TITLE, "Sales_Report_${dateRangeType.name}.csv")
            type = "text/csv"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Sales Report")
        context.startActivity(shareIntent)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Business Reports",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Comprehensive sales, profit, and audit reconciliation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { exportReportCsv() },
                        modifier = Modifier.testTag("export_report_csv_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export Report", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export")
                    }
                }
            }

            // Business Selector
            item {
                BusinessFilterChips(
                    selectedBusiness = selectedBusiness,
                    onSelectBusiness = { viewModel.setBusinessFilter(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Period Filter
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateRangeType.values().forEach { range ->
                        FilterChip(
                            selected = dateRangeType == range,
                            onClick = { viewModel.setDateRangeType(range) },
                            label = { Text(range.label) },
                            modifier = Modifier.testTag("report_range_${range.name.lowercase()}")
                        )
                    }
                }
            }

            // 1. Executive Summary Table Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sales & Profit Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        SummaryRow("Gross Sales", Formatters.formatCurrency(metrics.grossSales))
                        SummaryRow("Discounts Applied", "-${Formatters.formatCurrency(metrics.totalDiscounts)}", SecondaryAmber)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        SummaryRow("Net Sales", Formatters.formatCurrency(metrics.netSales), MaterialTheme.colorScheme.primary, isBold = true)
                        SummaryRow("Estimated Profit", Formatters.formatCurrency(metrics.estimatedProfit), SecondaryAmber, isBold = true)
                        SummaryRow("Overall Profit Margin", Formatters.formatPercent(metrics.profitMarginPercent), SecondaryAmber)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        SummaryRow("Total Collected Cash", Formatters.formatCurrency(metrics.totalCollected), StatusPaid)
                        SummaryRow("Outstanding Receivables", Formatters.formatCurrency(metrics.outstandingReceivables), StatusPending, isBold = true)
                        SummaryRow("Orders Count", "${metrics.ordersCount} orders")
                        SummaryRow("Total Items Sold", "${metrics.totalProductsSold} units")
                    }
                }
            }

            // 2. Business Comparison Summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Business Comparison",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Metric", modifier = Modifier.weight(1.2f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Engraving", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EngravingBrown)
                            Text("Sintra Board", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SintraBoardBlue)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                        ReportComparisonRow("Net Sales", Formatters.formatCurrency(businessComparison.first.netSales), Formatters.formatCurrency(businessComparison.second.netSales))
                        ReportComparisonRow("Profit", Formatters.formatCurrency(businessComparison.first.estimatedProfit), Formatters.formatCurrency(businessComparison.second.estimatedProfit))
                        ReportComparisonRow("Margin %", Formatters.formatPercent(businessComparison.first.profitMarginPercent), Formatters.formatPercent(businessComparison.second.profitMarginPercent))
                        ReportComparisonRow("Orders", "${businessComparison.first.ordersCount}", "${businessComparison.second.ordersCount}")
                        ReportComparisonRow("Items Sold", "${businessComparison.first.totalProductsSold}", "${businessComparison.second.totalProductsSold}")
                        ReportComparisonRow("Receivables", Formatters.formatCurrency(businessComparison.first.outstandingReceivables), Formatters.formatCurrency(businessComparison.second.outstandingReceivables))
                    }
                }
            }

            // 3. Product Sales Quantity & Revenue Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Product Quantity & Sales Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (productStats.isEmpty()) {
                            Text("No product sales in this period.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Product", modifier = Modifier.weight(1.5f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Qty", modifier = Modifier.weight(0.5f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Revenue", modifier = Modifier.weight(1f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                            productStats.forEach { stat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(stat.productName, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Text(stat.businessType.displayName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("${stat.totalQuantity}", modifier = Modifier.weight(0.5f), fontSize = 13.sp)
                                    Text(
                                        Formatters.formatCurrency(stat.totalSales),
                                        modifier = Modifier.weight(1f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Customer Sales Summary Table
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Customer Sales & Receivables Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (customerSalesSummary.isEmpty()) {
                            Text("No customer records for this period.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Customer", modifier = Modifier.weight(1.2f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Net Sales", modifier = Modifier.weight(1f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Balance", modifier = Modifier.weight(1f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                            customerSalesSummary.forEach { cust ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.2f)) {
                                        Text(cust.first, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Text("${cust.third.second} orders", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        Formatters.formatCurrency(cust.second.first),
                                        modifier = Modifier.weight(1f),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        Formatters.formatCurrency(cust.third.first),
                                        modifier = Modifier.weight(1f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (cust.third.first > 0.0) StatusPending else StatusPaid
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color? = null,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = color ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ReportComparisonRow(
    label: String,
    val1: String,
    val2: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(label, modifier = Modifier.weight(1.2f), fontSize = 12.sp)
        Text(val1, modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(val2, modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
