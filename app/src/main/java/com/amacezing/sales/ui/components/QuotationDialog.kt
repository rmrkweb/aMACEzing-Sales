package com.amacezing.sales.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.amacezing.sales.model.BusinessType
import com.amacezing.sales.model.Customer
import com.amacezing.sales.model.DiscountType
import com.amacezing.sales.model.Product
import com.amacezing.sales.model.QuotationLineItem
import com.amacezing.sales.util.Formatters

data class QuotationItemState(
    var productId: Long? = null,
    var productName: String = "",
    var quantity: Int = 1,
    var unitPrice: Double = 0.0,
    var notes: String = ""
) {
    val subtotal: Double
        get() = quantity * unitPrice
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotationDialog(
    availableProducts: List<Product>,
    existingCustomers: List<Customer>,
    defaultValidityDays: Int = 7,
    onDismiss: () -> Unit,
    onSaveQuotation: (
        businessType: BusinessType,
        customerName: String,
        customerPhone: String,
        customerEmail: String,
        customerAddress: String,
        validityDays: Int,
        items: List<QuotationLineItem>,
        discountType: DiscountType,
        discountValue: Double,
        remarks: String
    ) -> Unit
) {
    var selectedBusinessType by remember { mutableStateOf(BusinessType.ENGRAVING) }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var customerEmail by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var validityDays by remember { mutableIntStateOf(defaultValidityDays) }
    var discountType by remember { mutableStateOf(DiscountType.PERCENTAGE) }
    var discountValueText by remember { mutableStateOf("0") }
    var remarks by remember { mutableStateOf("") }

    // Line Items State
    val lineItems = remember {
        mutableStateListOf(
            QuotationItemState(productName = "", quantity = 1, unitPrice = 0.0)
        )
    }

    var showCustomerDropdown by remember { mutableStateOf(false) }

    val filteredProducts = remember(selectedBusinessType, availableProducts) {
        availableProducts.filter { it.businessType == selectedBusinessType }
    }

    // Calculations
    val grossTotal = lineItems.sumOf { it.subtotal }
    val discountVal = discountValueText.toDoubleOrNull() ?: 0.0
    val discountAmount = when (discountType) {
        DiscountType.PERCENTAGE -> (grossTotal * discountVal / 100.0).coerceAtLeast(0.0)
        DiscountType.FIXED -> discountVal.coerceAtMost(grossTotal).coerceAtLeast(0.0)
    }
    val netTotal = (grossTotal - discountAmount).coerceAtLeast(0.0)

    val isFormValid = customerName.trim().isNotBlank() &&
            lineItems.any { it.productName.trim().isNotBlank() && it.quantity > 0 && it.unitPrice >= 0 }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing)
                .widthIn(max = 640.dp)
                .fillMaxWidth(0.96f)
                .heightIn(max = 720.dp)
                .testTag("quotation_dialog"),
            shape = RoundedCornerShape(DialogDefaults.CornerRadius),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, com.amacezing.sales.ui.theme.AppBorder),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RequestQuote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate Price Quotation",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_quotation_dialog_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Division Selection
                    item {
                        Text("Business Division", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BusinessType.entries.forEach { bType ->
                                FilterChip(
                                    selected = selectedBusinessType == bType,
                                    onClick = { selectedBusinessType = bType },
                                    label = { Text(bType.displayName) },
                                    modifier = Modifier.testTag("quotation_biz_chip_${bType.name.lowercase()}")
                                )
                            }
                        }
                    }

                    // 2. Target Customer Info
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Target Customer Details", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                                    if (existingCustomers.isNotEmpty()) {
                                        Box {
                                            TextButton(
                                                onClick = { showCustomerDropdown = true },
                                                modifier = Modifier.testTag("select_existing_customer_button")
                                            ) {
                                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Select Existing")
                                            }

                                            DropdownMenu(
                                                expanded = showCustomerDropdown,
                                                onDismissRequest = { showCustomerDropdown = false }
                                            ) {
                                                existingCustomers.forEach { cust ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Column {
                                                                Text(cust.name, fontWeight = FontWeight.SemiBold)
                                                                if (cust.phone.isNotBlank()) {
                                                                    Text(cust.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                                }
                                                            }
                                                        },
                                                        onClick = {
                                                            customerName = cust.name
                                                            customerPhone = cust.phone
                                                            customerEmail = cust.email
                                                            customerAddress = cust.notes
                                                            showCustomerDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = customerName,
                                    onValueChange = { customerName = it },
                                    label = { Text("Customer / Company Name *") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("quotation_customer_name_input"),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customerPhone,
                                        onValueChange = { customerPhone = it },
                                        label = { Text("Contact Number") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                                    )

                                    OutlinedTextField(
                                        value = customerEmail,
                                        onValueChange = { customerEmail = it },
                                        label = { Text("Email Address") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = customerAddress,
                                    onValueChange = { customerAddress = it },
                                    label = { Text("Delivery / Business Address") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    // 3. Product Selections & Line Items
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Products & Services", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                            TextButton(
                                onClick = {
                                    lineItems.add(QuotationItemState(productName = "", quantity = 1, unitPrice = 0.0))
                                },
                                modifier = Modifier.testTag("add_quotation_item_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Custom Item")
                            }
                        }
                    }

                    itemsIndexed(lineItems) { index, itemState ->
                        var showProductPicker by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Item #${index + 1}", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                    Row {
                                        if (filteredProducts.isNotEmpty()) {
                                            Box {
                                                OutlinedButton(
                                                    onClick = { showProductPicker = true },
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Pick Catalog", fontSize = 11.sp)
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                                }

                                                DropdownMenu(
                                                    expanded = showProductPicker,
                                                    onDismissRequest = { showProductPicker = false }
                                                ) {
                                                    filteredProducts.forEach { prod ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                                ) {
                                                                    Text(prod.name)
                                                                    Spacer(modifier = Modifier.width(16.dp))
                                                                    Text(Formatters.formatCurrency(prod.sellingPrice), fontWeight = FontWeight.Bold)
                                                                }
                                                            },
                                                            onClick = {
                                                                itemState.productId = prod.id
                                                                itemState.productName = prod.name
                                                                itemState.unitPrice = prod.sellingPrice
                                                                showProductPicker = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (lineItems.size > 1) {
                                            IconButton(
                                                onClick = { lineItems.removeAt(index) },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = itemState.productName,
                                    onValueChange = {
                                        itemState.productName = it
                                        // trigger recomposition
                                        lineItems[index] = itemState.copy(productName = it)
                                    },
                                    label = { Text("Product / Service Description *") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = itemState.quantity.toString(),
                                        onValueChange = {
                                            val q = it.toIntOrNull() ?: 1
                                            lineItems[index] = itemState.copy(quantity = q)
                                        },
                                        label = { Text("Qty") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )

                                    OutlinedTextField(
                                        value = itemState.unitPrice.toString(),
                                        onValueChange = {
                                            val p = it.toDoubleOrNull() ?: 0.0
                                            lineItems[index] = itemState.copy(unitPrice = p)
                                        },
                                        label = { Text("Unit Price (₱)") },
                                        modifier = Modifier.weight(1.5f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                    )

                                    Column(
                                        modifier = Modifier.weight(1.5f),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text("Subtotal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(Formatters.formatCurrency(itemState.subtotal), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 4. Quotation Settings (Validity & Discounts)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Validity & Discounts", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = validityDays.toString(),
                                        onValueChange = { validityDays = it.toIntOrNull() ?: 7 },
                                        label = { Text("Validity (Days)") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )

                                    OutlinedTextField(
                                        value = discountValueText,
                                        onValueChange = { discountValueText = it },
                                        label = { Text(if (discountType == DiscountType.PERCENTAGE) "Discount (%)" else "Discount (₱)") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = discountType == DiscountType.PERCENTAGE,
                                        onClick = { discountType = DiscountType.PERCENTAGE },
                                        label = { Text("Percent %") }
                                    )
                                    FilterChip(
                                        selected = discountType == DiscountType.FIXED,
                                        onClick = { discountType = DiscountType.FIXED },
                                        label = { Text("Fixed ₱") }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = remarks,
                                    onValueChange = { remarks = it },
                                    label = { Text("Project Scope / Special Remarks (Optional)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // 5. Total Preview Summary
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Gross Total:", fontSize = 13.sp)
                                    Text(Formatters.formatCurrency(grossTotal), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                                if (discountAmount > 0.0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Discount Applied:", fontSize = 13.sp)
                                        Text("-${Formatters.formatCurrency(discountAmount)}", fontSize = 13.sp)
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Quotation Net Total:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        Formatters.formatCurrency(netTotal),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cancel_quotation_button")
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val items = lineItems.filter { it.productName.isNotBlank() && it.quantity > 0 }.map {
                                QuotationLineItem(
                                    quotationId = 0,
                                    productId = it.productId,
                                    productName = it.productName.trim(),
                                    quantity = it.quantity,
                                    unitPrice = it.unitPrice,
                                    lineSubtotal = it.subtotal,
                                    notes = it.notes
                                )
                            }
                            onSaveQuotation(
                                selectedBusinessType,
                                customerName.trim(),
                                customerPhone.trim(),
                                customerEmail.trim(),
                                customerAddress.trim(),
                                validityDays,
                                items,
                                discountType,
                                discountVal,
                                remarks.trim()
                            )
                        },
                        enabled = isFormValid,
                        modifier = Modifier
                            .weight(2f)
                            .testTag("submit_quotation_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Quotation")
                    }
                }
            }
        }
    }
}
