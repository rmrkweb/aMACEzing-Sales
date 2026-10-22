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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amacezing.sales.model.BusinessType
import com.amacezing.sales.ui.components.nameOf
import com.amacezing.sales.ui.theme.EngravingBrown
import com.amacezing.sales.ui.theme.SecondaryAmber
import com.amacezing.sales.ui.theme.SintraBoardBlue
import com.amacezing.sales.ui.theme.StatusCancelled
import com.amacezing.sales.ui.theme.StatusPaid
import com.amacezing.sales.ui.theme.StatusPending
import com.amacezing.sales.util.CsvHelper
import com.amacezing.sales.util.Formatters
import com.amacezing.sales.util.formatCurrency
import com.amacezing.sales.util.SampleMigrationData
import com.amacezing.sales.viewmodel.SalesViewModel

@Composable
fun ImportExportScreen(
    viewModel: SalesViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()

    val context = LocalContext.current
    val importText by viewModel.importText.collectAsState()
    val importBusinessType by viewModel.importBusinessType.collectAsState()
    val importSheetName by viewModel.importSheetName.collectAsState()
    val preview by viewModel.importPreview.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val businesses = com.amacezing.sales.ui.components.LocalBusinesses.current

    fun exportAllCsv() {
        val csv = CsvHelper.exportTransactionsToCsv(
            allTransactions,
            businesses.associate { it.id to it.name }
        )
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, csv)
            putExtra(Intent.EXTRA_TITLE, "AMacezing_All_Transactions.csv")
            type = "text/csv"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export All Data CSV")
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
                            text = "Excel Migration & Export",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Import historical sales workbooks or backup to CSV",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { exportAllCsv() },
                        modifier = Modifier.testTag("export_all_transactions_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export All", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export All")
                    }
                }
            }

            // Quick Preloaded Sample Workbooks Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "1-Click Historical Excel Worksheets",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Click to load and preview authentic sales monitoring sheets from your Excel files:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.loadSampleMigration(
                                        csvContent = SampleMigrationData.ENGRAVING_2026_CSV,
                                        businessType = com.amacezing.sales.model.Business.ENGRAVING_ID,
                                        sheetName = "Engraving 2026"
                                    )
                                },
                                modifier = Modifier.testTag("load_sample_engraving_2026")
                            ) {
                                Text("Engraving 2026", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.loadSampleMigration(
                                        csvContent = SampleMigrationData.SINTRA_BOARD_2025_CSV,
                                        businessType = com.amacezing.sales.model.Business.SINTRA_BOARD_ID,
                                        sheetName = "Sintra Board 2025"
                                    )
                                },
                                modifier = Modifier.testTag("load_sample_sintra_2025")
                            ) {
                                Text("Sintra Board 2025", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.loadSampleMigration(
                                        csvContent = SampleMigrationData.SINTRA_BOARD_2026_CSV,
                                        businessType = com.amacezing.sales.model.Business.SINTRA_BOARD_ID,
                                        sheetName = "Sintra Board 2026"
                                    )
                                },
                                modifier = Modifier.testTag("load_sample_sintra_2026")
                            ) {
                                Text("Sintra Board 2026", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Target Business and Sheet Name Selection
            item {
                Column {
                    Text("Target Business for Import", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // The workbook layouts below exist for the two original businesses only.
                        listOf(
                            com.amacezing.sales.model.Business.ENGRAVING_ID,
                            com.amacezing.sales.model.Business.SINTRA_BOARD_ID
                        ).forEach { bType ->
                            FilterChip(
                                selected = importBusinessType == bType,
                                onClick = { viewModel.importBusinessType.value = bType },
                                label = { Text(businesses.nameOf(bType)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importSheetName,
                        onValueChange = { viewModel.importSheetName.value = it },
                        label = { Text("Worksheet Name / Year Identifier") },
                        modifier = Modifier.fillMaxWidth().testTag("sheet_name_input")
                    )
                }
            }

            // CSV / Excel Content Input Area
            item {
                Column {
                    Text("Excel / CSV Text Data", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { viewModel.importText.value = it },
                        placeholder = {
                            Text(
                                if (importBusinessType == com.amacezing.sales.model.Business.ENGRAVING_ID)
                                    "Customer,Date,Bamboo Coaster,Bamboo Ballpen,Discount %,Total,Profit,Remarks"
                                else
                                    "Customer,Date,A4 Quantity,A3 Quantity,Discount %,Total,Profit,Remarks"
                            )
                        },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_text_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.previewImport() },
                            enabled = importText.isNotBlank(),
                            modifier = Modifier.testTag("preview_import_button")
                        ) {
                            Icon(Icons.Default.Preview, contentDescription = "Preview", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Validate & Preview")
                        }
                    }
                }
            }

            // Import Preview Box
            preview?.let { summary ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Import Validation Preview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Summary Badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Detected", fontSize = 11.sp)
                                        Text("${summary.totalDetected}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StatusPaid.copy(alpha = 0.15f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Valid Rows", fontSize = 11.sp, color = StatusPaid)
                                        Text("${summary.validCount}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = StatusPaid)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (summary.invalidCount > 0) StatusCancelled.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Invalid", fontSize = 11.sp, color = if (summary.invalidCount > 0) StatusCancelled else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${summary.invalidCount}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (summary.invalidCount > 0) StatusCancelled else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (summary.duplicateCount > 0) StatusPending.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Duplicates", fontSize = 11.sp, color = if (summary.duplicateCount > 0) StatusPending else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${summary.duplicateCount}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (summary.duplicateCount > 0) StatusPending else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            // Error report list if any
                            if (summary.errorReport.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Validation Issues Identified:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = StatusCancelled)
                                summary.errorReport.take(4).forEach { err ->
                                    Text("• $err", fontSize = 11.sp, color = StatusCancelled)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action button to confirm
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ready to migrate ${summary.validCount} transactions.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Button(
                                    onClick = { viewModel.executeImport() },
                                    enabled = summary.validCount > 0 && !isImporting,
                                    modifier = Modifier.testTag("confirm_import_button")
                                ) {
                                    if (isImporting) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Import ${summary.validCount} Rows")
                                    }
                                }
                            }
                        }
                    }
                }

                // Sample parsed rows preview
                items(summary.rows.take(6)) { row ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (row.isValid) MaterialTheme.colorScheme.surface else StatusCancelled.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Row ${row.rowNumber}: ${row.rawCustomer}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    if (row.isDuplicate) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("(Duplicate Match)", fontSize = 10.sp, color = StatusPending, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("Date: ${row.rawDate} • P1 Qty: ${row.product1Qty}, P2 Qty: ${row.product2Qty}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (row.rawRemarks.isNotBlank()) {
                                    Text("Remarks: ${row.rawRemarks}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(Formatters.formatCurrency(row.calculatedNet), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Profit: ${row.calculatedProfit.formatCurrency(settings.isProfitMasked)}", fontSize = 11.sp, color = SecondaryAmber)
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
