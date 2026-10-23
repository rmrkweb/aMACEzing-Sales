package com.amacezing.sales.ui.components

import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amacezing.sales.model.BusinessType
import com.amacezing.sales.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDialog(
    initialProduct: Product? = null,
    defaultBusinessType: BusinessType? = null,
    onSave: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    val businesses = LocalBusinesses.current
    var businessType by remember {
        mutableStateOf(
            initialProduct?.businessType
                ?: defaultBusinessType
                ?: businesses.selectable().firstOrNull()?.id
                ?: ""
        )
    }
    var businessDropdownExpanded by remember { mutableStateOf(false) }
    var sellingPriceText by remember { mutableStateOf(initialProduct?.sellingPrice?.toString() ?: "") }
    var unitCostText by remember { mutableStateOf(initialProduct?.unitCost?.toString() ?: "") }
    var isActive by remember { mutableStateOf(initialProduct?.isActive ?: true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(DialogDefaults.CornerRadius),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = {
            Text(
                text = if (initialProduct == null) "Add New Product" else "Edit Product",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                ExposedDropdownMenuBox(
                    expanded = businessDropdownExpanded,
                    onExpandedChange = { businessDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = businesses.nameOf(businessType),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Business *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = businessDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("product_business_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = businessDropdownExpanded,
                        onDismissRequest = { businessDropdownExpanded = false }
                    ) {
                        businesses.selectable().map { it.id }.forEach { bType ->
                            DropdownMenuItem(
                                text = { Text(businesses.nameOf(bType)) },
                                onClick = {
                                    businessType = bType
                                    businessDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Product Name *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = sellingPriceText,
                    onValueChange = {
                        sellingPriceText = it
                        errorMessage = null
                    },
                    label = { Text("Selling Price (₱) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_selling_price_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = unitCostText,
                    onValueChange = {
                        unitCostText = it
                        errorMessage = null
                    },
                    label = { Text("Unit Cost (₱)") },
                    supportingText = {
                        Text(
                            text = if (businesses.findById(businessType)?.profitMethod != com.amacezing.sales.model.ProfitMethod.FIXED_MARGIN)
                                "Captured on future sales. Profit = Selling Price - Cost."
                            else
                                "Optional for this business. If omitted, uses its configured margin %.",
                            fontSize = 11.sp
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_unit_cost_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active in catalog",
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp
                    )
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        modifier = Modifier.testTag("product_active_switch")
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            PrimaryActionButton(
                label = "Save Product",
                icon = Icons.Default.Check,
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Product name cannot be empty."
                        return@PrimaryActionButton
                    }
                    val selling = sellingPriceText.toDoubleOrNull()
                    if (selling == null || selling < 0.0) {
                        errorMessage = "Please enter a valid selling price."
                        return@PrimaryActionButton
                    }
                    val cost = unitCostText.toDoubleOrNull() ?: 0.0

                    val product = initialProduct?.copy(
                        name = name.trim(),
                        businessType = businessType,
                        sellingPrice = selling,
                        unitCost = cost,
                        isActive = isActive
                    ) ?: Product(
                        name = name.trim(),
                        businessType = businessType,
                        sellingPrice = selling,
                        unitCost = cost,
                        isActive = isActive
                    )
                    onSave(product)
                },
                modifier = Modifier.testTag("save_product_button")
            )
        },
        dismissButton = {
            DismissActionButton(
                label = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_product_button")
            )
        }
    )
}
