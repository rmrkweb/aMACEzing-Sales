package com.amacezing.sales.ui.screens

import com.amacezing.sales.util.formatCurrency

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amacezing.sales.model.BusinessType
import com.amacezing.sales.ui.components.BusinessBadge
import com.amacezing.sales.ui.components.BusinessComparisonCard
import com.amacezing.sales.ui.components.BusinessFilterChips
import com.amacezing.sales.ui.components.MetricCard
import com.amacezing.sales.ui.components.PaidVsUnpaidCard
import com.amacezing.sales.ui.components.PaymentStatusBadge
import com.amacezing.sales.ui.components.ProductSalesBreakdownCard
import com.amacezing.sales.ui.components.SalesAndProfitTrendChart
import com.amacezing.sales.ui.navigation.Screen
import com.amacezing.sales.ui.theme.SecondaryAmber
import com.amacezing.sales.ui.theme.StatusPaid
import com.amacezing.sales.ui.theme.StatusPending
import com.amacezing.sales.util.Formatters
import com.amacezing.sales.util.formatCurrency
import com.amacezing.sales.viewmodel.DateRangeType
import com.amacezing.sales.viewmodel.SalesViewModel

@Composable
fun DashboardScreen(
    viewModel: SalesViewModel,
    modifier: Modifier = Modifier
) {
    val selectedBusiness by viewModel.selectedBusiness.collectAsState()
    val dateRangeType by viewModel.dateRangeType.collectAsState()
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val outstandingTransactions by viewModel.outstandingTransactions.collectAsState()
    val productStats by viewModel.productSalesStats.collectAsState()
    val businessComparison by viewModel.businessComparison.collectAsState()
    val trendData by viewModel.monthlyTrendData.collectAsState()

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Image(
                            painter = painterResource(id = com.amacezing.sales.R.drawable.amacezing_logo),
                            contentDescription = "aMACEzing Printing Services logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(9.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "aMACEzing Sales",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Sales Monitoring & Profit Dashboard",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.showNewTransactionDialog.value = true },
                        modifier = Modifier.testTag("dashboard_new_order_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Order", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Order")
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

            // Date Range Chips
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
                            label = { Text(range.label, fontSize = 12.sp) },
                            leadingIcon = if (dateRangeType == range) {
                                { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            modifier = Modifier.testTag("date_range_${range.name.lowercase()}")
                        )
                    }
                }
            }

            // Top Primary Metric Cards Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Net Sales",
                        value = Formatters.formatCurrency(metrics.netSales),
                        icon = Icons.Default.AttachMoney,
                        iconColor = MaterialTheme.colorScheme.primary,
                        subtitle = "${metrics.ordersCount} orders recorded",
                        modifier = Modifier.weight(1f)
                    )

                    MetricCard(
                        title = "Estimated Profit",
                        value = metrics.estimatedProfit.formatCurrency(settings.isProfitMasked),
                        icon = Icons.Default.ShowChart,
                        iconColor = SecondaryAmber,
                        subtitle = "Margin: ${if (settings.isProfitMasked) "••••" else Formatters.formatPercent(metrics.profitMarginPercent)}",
                        subtitleColor = SecondaryAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Collected Cash",
                        value = Formatters.formatCurrency(metrics.totalCollected),
                        icon = Icons.Default.Payments,
                        iconColor = StatusPaid,
                        subtitle = "Paid into account",
                        subtitleColor = StatusPaid,
                        modifier = Modifier.weight(1f)
                    )

                    MetricCard(
                        title = "Outstanding",
                        value = Formatters.formatCurrency(metrics.outstandingReceivables),
                        icon = Icons.Default.Warning,
                        iconColor = StatusPending,
                        subtitle = "Receivables to collect",
                        subtitleColor = StatusPending,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Secondary Metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = "Gross Sales",
                        value = Formatters.formatCurrency(metrics.grossSales),
                        icon = Icons.Default.ShoppingBag,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Discounts",
                        value = Formatters.formatCurrency(metrics.totalDiscounts),
                        icon = Icons.Default.Discount,
                        iconColor = SecondaryAmber,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Avg Order",
                        value = Formatters.formatCurrency(metrics.averageOrderValue),
                        icon = Icons.Default.Receipt,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Visual Charts
            item {
                SalesAndProfitTrendChart(data = trendData, isProfitMasked = settings.isProfitMasked)
            }

            item {
                BusinessComparisonCard(
                    engravingMetrics = businessComparison.first,
                    sintraMetrics = businessComparison.second
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProductSalesBreakdownCard(
                        stats = productStats,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                PaidVsUnpaidCard(
                    totalCollected = metrics.totalCollected,
                    outstandingReceivables = metrics.outstandingReceivables
                )
            }

            // Outstanding Receivables Actionable Alert List
            if (outstandingTransactions.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Outstanding Receivables (${outstandingTransactions.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = {
                            viewModel.setStatusFilter(com.amacezing.sales.model.PaymentStatus.PENDING)
                            viewModel.navigateTo(Screen.TRANSACTIONS)
                        }) {
                            Text("View All")
                        }
                    }
                }

                items(outstandingTransactions.take(4)) { item ->
                    val tx = item.transaction
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.viewingTransaction.value = item }
                            .testTag("outstanding_item_${tx.orderNumber}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(tx.orderNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    BusinessBadge(tx.businessType)
                                }
                                Text(tx.customerName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(
                                    "Net: ${Formatters.formatCurrency(tx.netSales)} • Paid: ${Formatters.formatCurrency(tx.amountPaid)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    Formatters.formatCurrency(tx.outstandingBalance),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = StatusPending
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { viewModel.recordingPaymentTransaction.value = item },
                                    modifier = Modifier.testTag("quick_pay_${tx.orderNumber}")
                                ) {
                                    Text("Pay", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Recent Transactions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { viewModel.navigateTo(Screen.TRANSACTIONS) }) {
                        Text("See All (${filteredTransactions.size})")
                    }
                }
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No transactions recorded yet.", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Create an order or import from Excel migration to begin.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(onClick = { viewModel.showNewTransactionDialog.value = true }) {
                                Text("Create First Order")
                            }
                        }
                    }
                }
            } else {
                items(filteredTransactions.take(5)) { item ->
                    val tx = item.transaction
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.viewingTransaction.value = item }
                            .testTag("recent_order_${tx.orderNumber}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(tx.orderNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    BusinessBadge(tx.businessType)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    PaymentStatusBadge(tx.paymentStatus)
                                }
                                Text(tx.customerName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    Formatters.formatEpochMillisToDate(tx.orderDateEpochMillis),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    Formatters.formatCurrency(tx.netSales),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Profit: ${tx.estimatedProfit.formatCurrency(settings.isProfitMasked)}",
                                    fontSize = 11.sp,
                                    color = SecondaryAmber
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp)) // Padding for bottom bar / FAB
            }
        }
    }
}
