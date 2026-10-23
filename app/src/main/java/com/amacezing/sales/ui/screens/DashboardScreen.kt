package com.amacezing.sales.ui.screens

import com.amacezing.sales.util.formatCurrency

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import com.amacezing.sales.ui.theme.AppTextSecondary
import com.amacezing.sales.ui.theme.AppSurfaceElevated
import com.amacezing.sales.ui.theme.AppBorder
import com.amacezing.sales.ui.theme.AccentViolet
import com.amacezing.sales.ui.theme.AccentCyan
import com.amacezing.sales.ui.components.FilterSectionLabel
import com.amacezing.sales.ui.components.DateFilterBar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.platform.testTag
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
    val customStartMillis by viewModel.customStartDateMillis.collectAsState()
    val customEndMillis by viewModel.customEndDateMillis.collectAsState()
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val outstandingTransactions by viewModel.outstandingTransactions.collectAsState()
    val productStats by viewModel.productSalesStats.collectAsState()
    val businessComparison by viewModel.businessComparison.collectAsState()
    val trendData by viewModel.monthlyTrendData.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val businessList = com.amacezing.sales.ui.components.LocalBusinesses.current

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // First-run guidance until a business, a product and a sale exist
            if (businessList.isEmpty() || allProducts.isEmpty() || allTransactions.isEmpty()) {
                item {
                    GettingStartedCard(
                        hasBusiness = businessList.isNotEmpty(),
                        hasProduct = allProducts.isNotEmpty(),
                        hasSale = allTransactions.isNotEmpty(),
                        onAddBusiness = { viewModel.navigateTo(Screen.SETTINGS) },
                        onAddProduct = { viewModel.navigateTo(Screen.PRODUCTS) },
                        onNewSale = { viewModel.showNewTransactionDialog.value = true }
                    )
                }
            }

            // Header + filters: one coherent block (title, BUSINESS, PERIOD)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Dashboard",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AccentCyan
                )
                Text(
                    text = "Sales Monitoring & Profit Dashboard",
                    fontSize = 14.sp,
                    color = AppTextSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))

                BusinessFilterChips(
                    selectedBusiness = selectedBusiness,
                    onSelectBusiness = { viewModel.setBusinessFilter(it) },
                    modifier = Modifier.fillMaxWidth(),
                    showLabel = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                DateFilterBar(
                    selectedType = dateRangeType,
                    customStartMillis = customStartMillis,
                    customEndMillis = customEndMillis,
                    onSelectType = { viewModel.setDateRangeType(it) },
                    onApplyCustomRange = { start, end -> viewModel.applyCustomDateRange(start, end) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            FilterSectionLabel("Financial overview", modifier = Modifier.padding(top = 8.dp))
        }

        // Primary metrics (equal-height rows)
        item {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Net Sales",
                    value = Formatters.formatCurrency(metrics.netSales),
                    icon = Icons.Default.AttachMoney,
                    iconColor = AccentCyan,
                    subtitle = "${metrics.ordersCount} orders recorded",
                    subtitleColor = AccentCyan,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )

                MetricCard(
                    title = "Estimated Profit",
                    value = metrics.estimatedProfit.formatCurrency(settings.isProfitMasked),
                    icon = Icons.Default.ShowChart,
                    iconColor = AccentViolet,
                    subtitle = "Margin: ${if (settings.isProfitMasked) "••••" else Formatters.formatPercent(metrics.profitMarginPercent)}",
                    subtitleColor = AccentViolet,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Collected Cash",
                    value = Formatters.formatCurrency(metrics.totalCollected),
                    icon = Icons.Default.Payments,
                    iconColor = StatusPaid,
                    subtitle = "Paid into account",
                    subtitleColor = StatusPaid,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )

                MetricCard(
                    title = "Outstanding",
                    value = Formatters.formatCurrency(metrics.outstandingReceivables),
                    icon = Icons.Default.Warning,
                    iconColor = StatusPending,
                    subtitle = "Receivables to collect",
                    subtitleColor = StatusPending,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }

        // Secondary metrics (2 columns)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Gross Sales",
                        value = Formatters.formatCurrency(metrics.grossSales),
                        icon = Icons.Default.ShoppingBag,
                        iconColor = AccentCyan,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    MetricCard(
                        title = "Discounts",
                        value = Formatters.formatCurrency(metrics.totalDiscounts),
                        icon = Icons.Default.Discount,
                        iconColor = StatusPending,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Avg Order",
                        value = Formatters.formatCurrency(metrics.averageOrderValue),
                        icon = Icons.Default.Receipt,
                        iconColor = AccentCyan,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    MetricCard(
                        title = "Top Customer",
                        value = metrics.topCustomerName.ifEmpty { "—" },
                        icon = Icons.Default.EmojiEvents,
                        iconColor = StatusPending,
                        subtitle = if (metrics.topCustomerName.isEmpty()) null
                        else Formatters.formatCurrency(metrics.topCustomerSales),
                        subtitleColor = StatusPending,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }

        // Visual Charts
            item {
                SalesAndProfitTrendChart(data = trendData, isProfitMasked = settings.isProfitMasked)
            }

            item {
                BusinessComparisonCard(
                    comparison = businessComparison,
                    isProfitMasked = settings.isProfitMasked
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
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, AppBorder),
                        colors = CardDefaults.cardColors(containerColor = AppSurfaceElevated)
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
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, AppBorder),
                        color = AppSurfaceElevated,
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
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, AppBorder),
                        colors = CardDefaults.cardColors(containerColor = AppSurfaceElevated)
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

@Composable
private fun GettingStartedCard(
    hasBusiness: Boolean,
    hasProduct: Boolean,
    hasSale: Boolean,
    onAddBusiness: () -> Unit,
    onAddProduct: () -> Unit,
    onNewSale: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("getting_started_card"),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.amacezing.sales.ui.theme.AppBorder),
        colors = CardDefaults.cardColors(containerColor = com.amacezing.sales.ui.theme.AppSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Welcome to aMACEzing Sales", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "Set up your business in three quick steps. Your numbers appear here once you record sales.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            StartStep(1, "Add your first business", "Name, order prefix, colour and how profit is calculated.", hasBusiness, true, "Add business", onAddBusiness)
            StartStep(2, "Add your products or services", "Selling prices and costs power profit estimates.", hasProduct, hasBusiness, "Add products", onAddProduct)
            StartStep(3, "Record your first sale", "Create an order to see sales, profit and balances.", hasSale, hasBusiness, "New sale", onNewSale)
        }
    }
}

@Composable
private fun StartStep(
    number: Int,
    title: String,
    description: String,
    done: Boolean,
    enabled: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    if (done) StatusPaid.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = StatusPaid, modifier = Modifier.size(20.dp))
            } else {
                Text("$number", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!done) {
            androidx.compose.material3.OutlinedButton(
                onClick = onAction,
                enabled = enabled,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) { Text(actionLabel, fontSize = 12.sp) }
        }
    }
}
