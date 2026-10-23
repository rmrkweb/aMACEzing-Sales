package com.amacezing.sales.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.amacezing.sales.ui.theme.AppBorder
import com.amacezing.sales.ui.theme.SecondaryAmber
import com.amacezing.sales.ui.theme.StatusCancelled
import com.amacezing.sales.ui.theme.StatusPaid
import com.amacezing.sales.util.CsvHelper
import com.amacezing.sales.util.Formatters
import com.amacezing.sales.util.ImportTemplate
import com.amacezing.sales.viewmodel.SalesViewModel

private const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

private fun readUpload(context: android.content.Context, uri: Uri): Pair<String, ByteArray>? {
    val resolver = context.contentResolver
    val displayName: String? = try {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (_: Exception) {
        null
    }
    val name = displayName ?: uri.lastPathSegment ?: "import-file"
    // Read at most one byte over the limit so oversized files are rejected without loading them fully.
    val limit = 10 * 1024 * 1024 + 1
    val bytes: ByteArray = try {
        resolver.openInputStream(uri)?.use { input ->
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0
            while (total < limit) {
                val n = input.read(buffer, 0, minOf(buffer.size, limit - total))
                if (n < 0) break
                out.write(buffer, 0, n)
                total += n
            }
            out.toByteArray()
        }
    } catch (_: Exception) {
        null
    } ?: return null
    return name to bytes
}

private fun writeTo(context: android.content.Context, uri: Uri, bytes: ByteArray): Boolean = try {
    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } != null
} catch (_: Exception) { false }

@Composable
fun ImportExportScreen(
    viewModel: SalesViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allTransactions by viewModel.allTransactions.collectAsState()
    val businesses = com.amacezing.sales.ui.components.LocalBusinesses.current
    val plan by viewModel.importPlan.collectAsState()
    val fileName by viewModel.importFileName.collectAsState()
    val error by viewModel.importError.collectAsState()
    val includeDuplicates by viewModel.importIncludeDuplicates.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val upload = readUpload(context, uri)
            if (upload == null) viewModel.importError.value = "Couldn't open that file."
            else viewModel.loadImportFile(upload.first, upload.second)
        }
    }
    val saveCsvTemplate = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            val ok = writeTo(context, uri, ImportTemplate.csv().toByteArray(Charsets.UTF_8))
            viewModel.snackbarMessage.value = if (ok) "CSV template saved." else "Couldn't save the template."
        }
    }
    val saveXlsxTemplate = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(XLSX_MIME)) { uri ->
        if (uri != null) {
            val ok = writeTo(context, uri, ImportTemplate.xlsx())
            viewModel.snackbarMessage.value = if (ok) "Excel template saved." else "Couldn't save the template."
        }
    }

    fun exportAllCsv() {
        val csv = CsvHelper.exportTransactionsToCsv(allTransactions, businesses.associate { it.id to it.name })
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, csv)
            putExtra(Intent.EXTRA_TITLE, "AMacezing_All_Transactions.csv")
            type = "text/csv"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Export All Data CSV"))
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Import & Export",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Bring in past orders from a spreadsheet, or export your orders to CSV",
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
                        Text("Export")
                    }
                }
            }

            // Instructions
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, AppBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_instructions")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("How to import orders", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        listOf(
                            "1. Download a template (CSV or Excel) and fill it in. One row per product.",
                            "2. Save it as .csv or .xlsx and choose the file below.",
                            "3. Review the check results, then confirm the import."
                        ).forEach { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }

                        HorizontalDivider(color = AppBorder, modifier = Modifier.padding(vertical = 4.dp))
                        Text("Columns", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        listOf(
                            "Order Ref (optional) – rows sharing a ref become one order with several products",
                            "Order Date * – e.g. 2026-10-23 (day/month/year and Excel dates also work)",
                            "Customer *",
                            "Business * – must match a business you've added in Settings",
                            "Product *",
                            "Quantity * – whole number",
                            "Unit Price *",
                            "Unit Cost (optional) – blank uses the catalog cost",
                            "Discount % (optional) – 0 to 100, set on the first row of an order",
                            "Amount Paid (optional) – set on the first row of an order",
                            "Payment Method (optional) – default Cash",
                            "Remarks (optional)"
                        ).forEach { Text("• $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Text("* required", fontSize = 11.sp, color = SecondaryAmber)
                        Text(
                            "Totals, profit and payment status are calculated by the app, not read from the file. " +
                                "Nothing is saved until you confirm.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { saveCsvTemplate.launch("amacezing-orders-template.csv") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("download_csv_template")
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CSV template", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { saveXlsxTemplate.launch("amacezing-orders-template.xlsx") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("download_xlsx_template")
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Excel template", maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Upload
            item {
                Button(
                    onClick = {
                        pickFile.launch(
                            arrayOf(
                                "text/csv", "text/comma-separated-values", "application/csv",
                                "text/plain", XLSX_MIME, "application/octet-stream"
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn48()
                        .testTag("choose_import_file"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose CSV or Excel file", fontWeight = FontWeight.SemiBold)
                }
                fileName?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Selected: $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            error?.let { message ->
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StatusCancelled.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, StatusCancelled.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_error")
                    ) {
                        Text(message, modifier = Modifier.padding(12.dp), fontSize = 13.sp, color = StatusCancelled)
                    }
                }
            }

            plan?.let { result ->
                result.fatalProblem?.let { problem ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StatusCancelled.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, StatusCancelled.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("import_fatal_problem")
                        ) {
                            Text(problem, modifier = Modifier.padding(12.dp), fontSize = 13.sp, color = StatusCancelled)
                        }
                    }
                }

                if (result.fatalProblem == null) {
                    val readyCount = result.orders.count { includeDuplicates || !it.isDuplicate }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            SummaryTile("Ready", "$readyCount", StatusPaid, Modifier.weight(1f))
                            SummaryTile("Problems", "${result.invalidOrderCount}", if (result.invalidOrderCount > 0) StatusCancelled else MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
                            SummaryTile("Duplicates", "${result.duplicateCount}", if (result.duplicateCount > 0) SecondaryAmber else MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
                        }
                    }

                    if (result.duplicateCount > 0) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Include possible duplicates", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Same business, customer, date and total as an existing order. Skipped unless you turn this on.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = includeDuplicates,
                                    onCheckedChange = { viewModel.importIncludeDuplicates.value = it },
                                    modifier = Modifier.testTag("import_include_duplicates")
                                )
                            }
                        }
                    }

                    if (result.issues.isNotEmpty()) {
                        item {
                            Text("Problems found (these orders will be skipped)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StatusCancelled)
                        }
                        items(result.issues.take(20)) { issue ->
                            Text("Row ${issue.row}: ${issue.message}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (result.issues.size > 20) {
                            item { Text("…and ${result.issues.size - 20} more.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }

                    if (result.orders.isNotEmpty()) {
                        item { Text("Preview", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        items(result.orders.take(6)) { order ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, AppBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(order.customer, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            "${order.businessName} • ${Formatters.formatEpochMillisToDate(order.dateMillis)} • ${order.lines.size} item${if (order.lines.size == 1) "" else "s"}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (order.isDuplicate) {
                                            Text("Possible duplicate", fontSize = 10.sp, color = SecondaryAmber, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(Formatters.formatCurrency(order.net), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                        if (result.orders.size > 6) {
                            item { Text("…and ${result.orders.size - 6} more orders.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { viewModel.clearImport() }, enabled = !isImporting) { Text("Clear") }
                        Button(
                            onClick = { viewModel.executeImport() },
                            enabled = result.fatalProblem == null &&
                                result.orders.any { includeDuplicates || !it.isDuplicate } && !isImporting,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn48()
                                .testTag("confirm_import_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                val n = result.orders.count { includeDuplicates || !it.isDuplicate }
                                Text("Import $n order${if (n == 1) "" else "s"}", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(70.dp)) }
        }
    }
}

private fun Modifier.heightIn48(): Modifier = this.then(Modifier.height(48.dp))

@Composable
private fun SummaryTile(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, fontSize = 11.sp, color = color)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        }
    }
}
