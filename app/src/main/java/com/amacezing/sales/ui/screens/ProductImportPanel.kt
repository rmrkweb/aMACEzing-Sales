package com.amacezing.sales.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amacezing.sales.ui.components.CompactFilterChip
import com.amacezing.sales.ui.components.DismissActionButton
import com.amacezing.sales.ui.components.FilterSectionLabel
import com.amacezing.sales.ui.components.LocalBusinesses
import com.amacezing.sales.ui.components.PrimaryActionButton
import com.amacezing.sales.ui.components.SecondaryActionButton
import com.amacezing.sales.ui.components.businessIcon
import com.amacezing.sales.ui.components.parseBusinessColor
import com.amacezing.sales.ui.components.selectable
import com.amacezing.sales.ui.theme.AppBorder
import com.amacezing.sales.ui.theme.SecondaryAmber
import com.amacezing.sales.ui.theme.StatusCancelled
import com.amacezing.sales.ui.theme.StatusPaid
import com.amacezing.sales.util.Formatters
import com.amacezing.sales.util.ProductImportRow
import com.amacezing.sales.util.ProductImportTemplate
import com.amacezing.sales.viewmodel.SalesViewModel

private const val PREVIEW_LIMIT = 8
private const val ISSUE_LIMIT = 20

/**
 * Product import: pick a business, upload a CSV/Excel file, review the checks, then import.
 * State and saving live in [SalesViewModel]; this only renders them.
 */
@Composable
fun ProductImportPanel(
    viewModel: SalesViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val businesses = LocalBusinesses.current
    val selectableBusinesses = businesses.selectable()
    val selectedBusiness by viewModel.selectedBusiness.collectAsState()
    val businessId by viewModel.productImportBusinessId.collectAsState()
    val fileName by viewModel.productImportFileName.collectAsState()
    val plan by viewModel.productImportPlan.collectAsState()
    val error by viewModel.productImportError.collectAsState()
    val updateExisting by viewModel.productImportUpdateExisting.collectAsState()
    val isImporting by viewModel.isImportingProducts.collectAsState()

    // Only pre-select when the choice is obvious, so products never land in the wrong business by default.
    LaunchedEffect(selectableBusinesses, selectedBusiness) {
        if (businessId == null) {
            val preset = selectedBusiness?.takeIf { id -> selectableBusinesses.any { it.id == id } }
                ?: selectableBusinesses.singleOrNull()?.id
            if (preset != null) viewModel.selectProductImportBusiness(preset)
        }
    }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val upload = readUpload(context, uri)
            if (upload == null) viewModel.productImportError.value = "Couldn't open that file."
            else viewModel.loadProductImportFile(upload.first, upload.second)
        }
    }
    val saveCsvTemplate = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            val ok = writeTo(context, uri, ProductImportTemplate.csv().toByteArray(Charsets.UTF_8))
            viewModel.snackbarMessage.value = if (ok) "CSV template saved." else "Couldn't save the template."
        }
    }
    val saveXlsxTemplate = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(XLSX_MIME)) { uri ->
        if (uri != null) {
            val ok = writeTo(context, uri, ProductImportTemplate.xlsx())
            viewModel.snackbarMessage.value = if (ok) "Excel template saved." else "Couldn't save the template."
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Business
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterSectionLabel("Business *")
            if (selectableBusinesses.isEmpty()) {
                Text(
                    "Add a business in Settings before importing products.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("product_import_no_business")
                )
            } else {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    selectableBusinesses.forEach { business ->
                        CompactFilterChip(
                            label = business.name,
                            selected = businessId == business.id,
                            onClick = { viewModel.selectProductImportBusiness(business.id) },
                            selectedColor = parseBusinessColor(business.colorHex),
                            leadingIcon = businessIcon(business.iconKey),
                            modifier = Modifier.testTag("product_import_business_${business.id.lowercase()}")
                        )
                    }
                }
                Text(
                    "Every product in the file is added to the business you choose.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Instructions + templates
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, AppBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("product_import_instructions")
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("How to import products", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                listOf(
                    "1. Choose the business above.",
                    "2. Download a template (CSV or Excel) and fill it in. One row per product.",
                    "3. Save it as .csv or .xlsx and choose the file below.",
                    "4. Review the check results, then confirm the import."
                ).forEach { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }

                HorizontalDivider(color = AppBorder, modifier = Modifier.padding(vertical = 4.dp))
                Text("Columns", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                listOf(
                    "Product Name * – can't be empty",
                    "Selling Price * – a number, 0 or more",
                    "Unit Cost (optional) – a number, 0 or more; blank is saved as 0",
                    "Active (optional) – Yes or No; blank means Yes"
                ).forEach { Text("• $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text("* required", fontSize = 11.sp, color = SecondaryAmber)
                Text(
                    "Nothing is saved until you confirm. Imported products are also uploaded to your cloud account.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryActionButton(
                        label = "CSV template",
                        icon = Icons.Default.FileDownload,
                        onClick = { saveCsvTemplate.launch("amacezing-products-template.csv") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("download_product_csv_template")
                    )
                    SecondaryActionButton(
                        label = "Excel template",
                        icon = Icons.Default.FileDownload,
                        onClick = { saveXlsxTemplate.launch("amacezing-products-template.xlsx") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("download_product_xlsx_template")
                    )
                }
            }
        }

        // 3. Upload
        Column {
            PrimaryActionButton(
                label = "Choose CSV or Excel file",
                icon = Icons.Default.FileUpload,
                enabled = businessId != null && !isImporting,
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
                    .testTag("choose_product_import_file")
            )
            if (businessId == null && selectableBusinesses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Choose a business first.", fontSize = 12.sp, color = SecondaryAmber)
            }
            fileName?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Selected: $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        error?.let { ErrorBox(it, "product_import_error") }

        plan?.let { result ->
            result.fatalProblem?.let { ErrorBox(it, "product_import_fatal_problem") }

            if (result.fatalProblem == null) {
                val toSave = result.rowsToSave(updateExisting)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SummaryTile("New", "${result.newCount}", StatusPaid, Modifier.weight(1f))
                    SummaryTile(
                        "In catalog",
                        "${result.existingCount}",
                        if (result.existingCount > 0) SecondaryAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        Modifier.weight(1f)
                    )
                    SummaryTile(
                        "Problems",
                        "${result.invalidRowCount}",
                        if (result.invalidRowCount > 0) StatusCancelled else MaterialTheme.colorScheme.onSurfaceVariant,
                        Modifier.weight(1f)
                    )
                }

                if (result.existingCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Update existing products", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Same name in this business: use the file's price, cost and status. Skipped unless you turn this on.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = updateExisting,
                            onCheckedChange = { viewModel.productImportUpdateExisting.value = it },
                            modifier = Modifier.testTag("product_import_update_existing")
                        )
                    }
                }

                if (result.issues.isNotEmpty()) {
                    Text(
                        "Problems found (these rows will be skipped)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = StatusCancelled
                    )
                    result.issues.take(ISSUE_LIMIT).forEach { issue ->
                        Text(
                            "Row ${issue.row}: ${issue.message}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (result.issues.size > ISSUE_LIMIT) {
                        Text(
                            "…and ${result.issues.size - ISSUE_LIMIT} more.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (result.rows.isNotEmpty()) {
                    Text("Preview", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    result.rows.take(PREVIEW_LIMIT).forEach { row -> PreviewRow(row, updateExisting) }
                    if (result.rows.size > PREVIEW_LIMIT) {
                        Text(
                            "…and ${result.rows.size - PREVIEW_LIMIT} more products.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    DismissActionButton(
                        label = "Clear",
                        onClick = { viewModel.clearProductImport() },
                        enabled = !isImporting,
                        modifier = Modifier.testTag("clear_product_import_button")
                    )
                    PrimaryActionButton(
                        label = if (isImporting) "Importing…" else "Import ${toSave.size} product${if (toSave.size == 1) "" else "s"}",
                        icon = Icons.Default.Check,
                        enabled = toSave.isNotEmpty() && !isImporting,
                        onClick = { viewModel.executeProductImport() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_product_import_button")
                    )
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    DismissActionButton(
                        label = "Clear",
                        onClick = { viewModel.clearProductImport() },
                        modifier = Modifier.testTag("clear_product_import_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorBox(message: String, tag: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = StatusCancelled.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, StatusCancelled.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Text(message, modifier = Modifier.padding(12.dp), fontSize = 13.sp, color = StatusCancelled)
    }
}

@Composable
private fun PreviewRow(row: ProductImportRow, updateExisting: Boolean) {
    // The state is spelled out in text so it never relies on color alone.
    val (stateLabel, stateColor) = when {
        row.existingId == null -> "New" to StatusPaid
        updateExisting -> "Will update" to SecondaryAmber
        else -> "In catalog – skipped" to SecondaryAmber
    }
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
                Text(row.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "Cost ${Formatters.formatCurrency(row.unitCost)} • ${if (row.isActive) "Active" else "Inactive"}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(stateLabel, fontSize = 10.sp, color = stateColor, fontWeight = FontWeight.Bold)
            }
            Text(Formatters.formatCurrency(row.sellingPrice), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}
