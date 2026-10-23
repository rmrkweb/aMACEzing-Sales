package com.amacezing.sales.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import com.amacezing.sales.ui.components.nameOf
import com.amacezing.sales.model.QuotationStatus
import com.amacezing.sales.model.QuotationWithItems
import com.amacezing.sales.pdf.PdfDocumentGenerator
import com.amacezing.sales.pdf.PdfHelper
import com.amacezing.sales.ui.components.BusinessBadge
import com.amacezing.sales.ui.components.CardActionBar
import com.amacezing.sales.ui.components.CardActionButton
import com.amacezing.sales.ui.theme.SecondaryAmber
import com.amacezing.sales.ui.theme.StatusPaid
import com.amacezing.sales.ui.theme.StatusPending
import com.amacezing.sales.util.Formatters
import com.amacezing.sales.viewmodel.GeneratedPdfState
import com.amacezing.sales.viewmodel.SalesViewModel

@Composable
fun QuotationsScreen(
    viewModel: SalesViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val quotations by viewModel.allQuotations.collectAsState()
    val businessProfile by viewModel.businessProfile.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedBusinessType by remember { mutableStateOf<BusinessType?>(null) }
    var selectedStatus by remember { mutableStateOf<QuotationStatus?>(null) }

    var quotationToDelete by remember { mutableStateOf<QuotationWithItems?>(null) }
    var quotationToConvert by remember { mutableStateOf<QuotationWithItems?>(null) }

    val filteredQuotations = remember(quotations, searchQuery, selectedBusinessType, selectedStatus) {
        quotations.filter { qWithItems ->
            val q = qWithItems.quotation
            val matchesSearch = searchQuery.isBlank() ||
                    q.customerName.contains(searchQuery, ignoreCase = true) ||
                    q.quotationNumber.contains(searchQuery, ignoreCase = true)
            val matchesType = selectedBusinessType == null || q.businessType == selectedBusinessType
            val matchesStatus = selectedStatus == null || q.status == selectedStatus
            matchesSearch && matchesType && matchesStatus
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("quotations_screen")) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
           // Header
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Price Quotations",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Generate professional A4 PDF estimates with your business branding",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Summary Metric Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val totalGross = quotations.sumOf { it.quotation.netTotal }
                    val activeCount = quotations.count { it.quotation.status == QuotationStatus.SENT || it.quotation.status == QuotationStatus.DRAFT }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Total Quotes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${quotations.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Active: $activeCount", fontSize = 10.sp, color = StatusPending)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1.5f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Quoted Volume", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                Formatters.formatCurrency(totalGross),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text("Converted: ${quotations.count { it.quotation.status == QuotationStatus.CONVERTED }}", fontSize = 10.sp, color = StatusPaid)
                        }
                    }
                }
            }

            // Search Bar & Filter Chips
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by customer name or quotation #...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quotation_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedBusinessType == null,
                        onClick = { selectedBusinessType = null },
                        label = { Text("All Divisions", fontSize = 11.sp) }
                    )
                    com.amacezing.sales.ui.components.LocalBusinesses.current.forEach { business ->
                        FilterChip(
                            selected = selectedBusinessType == business.id,
                            onClick = { selectedBusinessType = business.id },
                            label = { Text(business.name, fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Quotations List
            if (filteredQuotations.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.RequestQuote,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (quotations.isEmpty()) "No price quotations yet" else "No matching quotations found",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Create a new quotation to generate professional branded A4 PDFs for prospective clients.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.showNewQuotationDialog.value = true },
                                modifier = Modifier.testTag("empty_state_create_quotation_button")
                            ) {
                                Text("Create First Quotation")
                            }
                        }
                    }
                }
            } else {
                items(filteredQuotations, key = { it.quotation.id }) { qWithItems ->
                    val q = qWithItems.quotation
                    val items = qWithItems.lineItems
                    var statusMenuExpanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quotation_card_${q.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Top Row: Number, Division badge, and Status Chip
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = q.quotationNumber,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    BusinessBadge(q.businessType)
                                }

                                // Status Dropdown Chip
                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when (q.status) {
                                            QuotationStatus.CONVERTED, QuotationStatus.ACCEPTED -> StatusPaid.copy(alpha = 0.15f)
                                            QuotationStatus.SENT -> MaterialTheme.colorScheme.primaryContainer
                                            QuotationStatus.DRAFT -> MaterialTheme.colorScheme.surfaceVariant
                                            QuotationStatus.DECLINED -> MaterialTheme.colorScheme.errorContainer
                                        },
                                        modifier = Modifier.clickable { statusMenuExpanded = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = q.status.displayName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (q.status) {
                                                    QuotationStatus.CONVERTED, QuotationStatus.ACCEPTED -> StatusPaid
                                                    QuotationStatus.SENT -> MaterialTheme.colorScheme.primary
                                                    QuotationStatus.DRAFT -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    QuotationStatus.DECLINED -> MaterialTheme.colorScheme.error
                                                }
                                            )
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = statusMenuExpanded,
                                        onDismissRequest = { statusMenuExpanded = false }
                                    ) {
                                        QuotationStatus.entries.forEach { st ->
                                            DropdownMenuItem(
                                                text = { Text(st.displayName) },
                                                onClick = {
                                                    viewModel.updateQuotationStatus(q.id, st)
                                                    statusMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Customer & Dates
                            Text(
                                text = q.customerName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )

                            if (q.customerPhone.isNotBlank() || q.customerEmail.isNotBlank()) {
                                val contactStr = listOfNotNull(
                                    q.customerPhone.takeIf { it.isNotBlank() },
                                    q.customerEmail.takeIf { it.isNotBlank() }
                                ).joinToString(" • ")
                                Text(
                                    text = contactStr,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Items overview summary
                            Text(
                                text = "${items.size} item(s): " + items.joinToString(", ") { "${it.quantity}x ${it.productName}" },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Bottom Row: Date & Net Total & Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Issued: ${Formatters.formatEpochMillisToDate(q.quotationDateEpochMillis)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Valid Until: ${Formatters.formatEpochMillisToDate(q.validUntilEpochMillis)}",
                                        fontSize = 11.sp,
                                        color = SecondaryAmber
                                    )
                                }

                                Text(
                                    text = Formatters.formatCurrency(q.netTotal),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Shared card action bar (icon + label), same style across all screens.
                            CardActionBar {
                                CardActionButton(
                                    label = "PDF",
                                    icon = Icons.Default.PictureAsPdf,
                                    onClick = {
                                        try {
                                            val pdfFile = PdfDocumentGenerator.generateQuotationPdf(
                                                context = context,
                                                quotationWithItems = qWithItems,
                                                profile = businessProfile,
                                                businessName = viewModel.businesses.value.nameOf(q.businessType)
                                            )
                                            viewModel.activePdfDialog.value = GeneratedPdfState(
                                                file = pdfFile,
                                                title = "Price Quotation",
                                                documentNumber = q.quotationNumber
                                            )
                                        } catch (e: Exception) {
                                            viewModel.snackbarMessage.value = "Failed to generate PDF: ${e.message}"
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("generate_quotation_pdf_button_${q.id}")
                                )

                                if (q.status != QuotationStatus.CONVERTED) {
                                    CardActionButton(
                                        label = "Convert",
                                        icon = Icons.Default.ShoppingCart,
                                        onClick = { quotationToConvert = qWithItems },
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .testTag("convert_quotation_button_${q.id}")
                                    )
                                    CardActionButton(
                                        label = "Edit",
                                        icon = Icons.Default.Edit,
                                        onClick = { viewModel.editingQuotation.value = qWithItems },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("edit_quotation_button_${q.id}")
                                    )
                                }

                                CardActionButton(
                                    label = "Delete",
                                    icon = Icons.Default.Delete,
                                    color = MaterialTheme.colorScheme.error,
                                    onClick = { quotationToDelete = qWithItems },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("delete_quotation_button_${q.id}")
                                )
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
            onClick = { viewModel.showNewQuotationDialog.value = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_new_quotation"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Quotation")
        }
    }

    // Convert Quotation to Order Dialog
    quotationToConvert?.let { target ->
        AlertDialog(
            onDismissRequest = { quotationToConvert = null },
            title = { Text("Convert Quotation to Order?") },
            text = {
                Column {
                    Text("This will create a new official sales transaction for ${target.quotation.customerName} with ${target.lineItems.size} line items totaling ${Formatters.formatCurrency(target.quotation.netTotal)}.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("The quotation status will be updated to 'Converted to Order'.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.convertQuotationToOrder(target)
                        quotationToConvert = null
                    },
                    modifier = Modifier.testTag("confirm_convert_quotation_button")
                ) {
                    Text("Convert to Order")
                }
            },
            dismissButton = {
                TextButton(onClick = { quotationToConvert = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    quotationToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { quotationToDelete = null },
            title = { Text("Delete Quotation?") },
            text = {
                Text("Are you sure you want to permanently delete quotation ${target.quotation.quotationNumber} for ${target.quotation.customerName}?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteQuotation(target.quotation.id)
                        quotationToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_quotation_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { quotationToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
