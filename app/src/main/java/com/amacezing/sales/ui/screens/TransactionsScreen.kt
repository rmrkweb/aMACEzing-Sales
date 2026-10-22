package com.amacezing.sales.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amacezing.sales.model.BusinessType
import com.amacezing.sales.model.PaymentStatus
import com.amacezing.sales.ui.components.BusinessBadge
import com.amacezing.sales.ui.components.BusinessFilterChips
import com.amacezing.sales.ui.components.PaymentStatusBadge
import com.amacezing.sales.ui.theme.SecondaryAmber
import com.amacezing.sales.ui.theme.StatusCancelled
import com.amacezing.sales.ui.theme.StatusPaid
import com.amacezing.sales.ui.theme.StatusPending
import com.amacezing.sales.util.CsvHelper
import com.amacezing.sales.util.Formatters
import com.amacezing.sales.util.formatCurrency
import com.amacezing.sales.viewmodel.SalesViewModel

@Composable
fun TransactionsScreen(
    viewModel: SalesViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transactions by viewModel.transactionsScreenList.collectAsState()
    val selectedBusiness by viewModel.selectedBusiness.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var sortByDateDesc by remember { mutableStateOf(true) }

    val sortedTransactions = remember(transactions, sortByDateDesc) {
        if (sortByDateDesc) {
            transactions.sortedByDescending { it.transaction.orderDateEpochMillis }
        } else {
            transactions.sortedByDescending { it.transaction.netSales }
        }
    }

    val businessList = com.amacezing.sales.ui.components.LocalBusinesses.current

    fun shareCsvExport() {
        val csv = CsvHelper.exportTransactionsToCsv(
            sortedTransactions,
            businessList.associate { it.id to it.name }
        )
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, csv)
            putExtra(Intent.EXTRA_TITLE, "Sales_Transactions_Export.csv")
            type = "text/csv"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export Transactions CSV")
        context.startActivity(shareIntent)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            text = "Sales Transactions",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${sortedTransactions.size} orders matching filters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { shareCsvExport() },
                        modifier = Modifier.testTag("export_transactions_csv_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export CSV", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CSV")
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

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search by Order #, Customer, or Product...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transactions_search_input")
                )
            }

            // Payment Status Filters & Sort
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = statusFilter == null,
                        onClick = { viewModel.setStatusFilter(null) },
                        label = { Text("All Statuses") },
                        modifier = Modifier.testTag("status_filter_all")
                    )

                    PaymentStatus.values().forEach { st ->
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { viewModel.setStatusFilter(st) },
                            label = { Text(st.displayName) },
                            modifier = Modifier.testTag("status_filter_${st.name.lowercase()}")
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    FilterChip(
                        selected = !sortByDateDesc,
                        onClick = { sortByDateDesc = !sortByDateDesc },
                        label = { Text(if (sortByDateDesc) "Sort: Date ↓" else "Sort: Net Sales ↓") },
                        leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }
            }

            // List of Transactions
            if (sortedTransactions.isEmpty()) {
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
                            Text("No transactions match the criteria.", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Try clearing the search query or status filter.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(sortedTransactions) { item ->
                    val tx = item.transaction
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.viewingTransaction.value = item }
                            .testTag("tx_card_${tx.orderNumber}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Row: Order #, Business, Status, Date
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tx.orderNumber,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    BusinessBadge(tx.businessType)
                                }
                                PaymentStatusBadge(tx.paymentStatus)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Customer & Date
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = tx.customerName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = Formatters.formatEpochMillisToDate(tx.orderDateEpochMillis),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Items summary
                            val itemsSummary = item.lineItems.joinToString(", ") { "${it.productName} (x${it.quantity})" }
                            if (itemsSummary.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = itemsSummary,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Financials Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = "Net: ${Formatters.formatCurrency(tx.netSales)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Profit: ${tx.estimatedProfit.formatCurrency(settings.isProfitMasked)}",
                                        fontSize = 12.sp,
                                        color = SecondaryAmber
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Paid: ${Formatters.formatCurrency(tx.amountPaid)}",
                                        fontSize = 12.sp,
                                        color = StatusPaid
                                    )
                                    if (tx.outstandingBalance > 0.0) {
                                        Text(
                                            text = "Bal: ${Formatters.formatCurrency(tx.outstandingBalance)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = StatusPending
                                        )
                                    }
                                }
                            }

                            // Action buttons row on card
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (tx.outstandingBalance > 0.0 && tx.paymentStatus != PaymentStatus.CANCELLED) {
                                    TextButton(
                                        onClick = { viewModel.recordingPaymentTransaction.value = item },
                                        modifier = Modifier.testTag("tx_pay_btn_${tx.orderNumber}")
                                    ) {
                                        Icon(Icons.Default.Payment, contentDescription = "Pay", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Record Pay", fontSize = 11.sp)
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        // Duplicate
                                        viewModel.editingTransaction.value = item
                                    },
                                    modifier = Modifier.size(32.dp).testTag("tx_edit_btn_${tx.orderNumber}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = { viewModel.itemToDelete.value = tx },
                                    modifier = Modifier.size(32.dp).testTag("tx_delete_btn_${tx.orderNumber}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusCancelled, modifier = Modifier.size(16.dp))
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

        // Floating Action Button
        FloatingActionButton(
            onClick = { viewModel.showNewTransactionDialog.value = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("transactions_add_order_button"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Order")
        }
    }
}
