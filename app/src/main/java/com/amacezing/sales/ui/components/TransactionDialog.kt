package com.amacezing.sales.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.amacezing.sales.model.BusinessType
import com.amacezing.sales.model.Customer
import com.amacezing.sales.model.DiscountType
import com.amacezing.sales.model.PaymentStatus
import com.amacezing.sales.model.Product
import com.amacezing.sales.model.TransactionWithDetails
import com.amacezing.sales.ui.theme.SecondaryAmber
import com.amacezing.sales.ui.theme.StatusPaid
import com.amacezing.sales.ui.theme.StatusPending
import com.amacezing.sales.util.Formatters
import com.amacezing.sales.util.formatCurrency
import com.amacezing.sales.util.LineItemInput
import com.amacezing.sales.util.SalesCalculator

/**
 * Form state for one order line. Each field is Compose state so edits (typing in Qty / Unit Price,
 * picking a product) trigger recomposition. With plain properties the text fields never refreshed,
 * which made them look read-only.
 */
class LineItemFormState(
    productId: Long,
    productName: String,
    quantityText: String,
    unitPriceText: String,
    unitCost: Double
) {
    var productId by mutableLongStateOf(productId)
    var productName by mutableStateOf(productName)
    var quantityText by mutableStateOf(quantityText)
    var unitPriceText by mutableStateOf(unitPriceText)
    var unitCost by mutableDoubleStateOf(unitCost)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDialog(
    initialTransaction: TransactionWithDetails? = null,
    isDuplicateMode: Boolean = false,
    availableProducts: List<Product>,
    existingCustomers: List<Customer>,
    defaultBusinessType: BusinessType = BusinessType.ENGRAVING,
    sintraMarginPercent: Double = 58.3333,
    sintraUseCostCalculation: Boolean = false,
    isProfitMasked: Boolean = false,
    onSaveNew: (
        businessType: BusinessType,
        customerName: String,
        orderDateMillis: Long,
        lineItems: List<LineItemInput>,
        productInfos: List<Pair<Long, String>>,
        discountType: DiscountType,
        discountValue: Double,
        initialPayment: Double,
        paymentMethod: String,
        paymentNotes: String,
        remarks: String
    ) -> Unit,
    onSaveEdit: (
        txWithDetails: TransactionWithDetails,
        lineItems: List<LineItemInput>,
        productInfos: List<Pair<Long, String>>,
        discountType: DiscountType,
        discountValue: Double,
        remarks: String,
        statusOverride: PaymentStatus?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditMode = initialTransaction != null && !isDuplicateMode

    var businessType by remember {
        mutableStateOf(initialTransaction?.transaction?.businessType ?: defaultBusinessType)
    }

    var customerName by remember {
        mutableStateOf(initialTransaction?.transaction?.customerName ?: "")
    }

    var orderDateMillis by remember {
        mutableLongStateOf(
            if (isDuplicateMode) System.currentTimeMillis()
            else initialTransaction?.transaction?.orderDateEpochMillis ?: System.currentTimeMillis()
        )
    }

    var discountType by remember {
        mutableStateOf(initialTransaction?.transaction?.discountType ?: DiscountType.PERCENTAGE)
    }

    var discountValueText by remember {
        mutableStateOf(
            initialTransaction?.let {
                if (it.transaction.discountType == DiscountType.PERCENTAGE) it.transaction.discountPercentage.toString()
                else it.transaction.discountAmount.toString()
            } ?: "0"
        )
    }

    var initialPaymentText by remember {
        mutableStateOf(if (isEditMode) initialTransaction?.transaction?.amountPaid?.toString() ?: "0" else "0")
    }

    var paymentMethod by remember { mutableStateOf("Cash") }
    var remarks by remember { mutableStateOf(initialTransaction?.transaction?.remarks ?: "") }
    var statusOverride by remember { mutableStateOf<PaymentStatus?>(initialTransaction?.transaction?.paymentStatus) }

    var showDatePicker by remember { mutableStateOf(false) }
    var customerSuggestionsExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Line items list
    val formLineItems = remember {
        mutableStateListOf<LineItemFormState>().apply {
            if (initialTransaction != null && initialTransaction.lineItems.isNotEmpty()) {
                addAll(
                    initialTransaction.lineItems.map { item ->
                        LineItemFormState(
                            productId = item.productId,
                            productName = item.productName,
                            quantityText = item.quantity.toString(),
                            unitPriceText = item.unitSellingPrice.toString(),
                            unitCost = item.unitCost
                        )
                    }
                )
            } else {
                // Initialize with first available product of the business
                val firstProduct = availableProducts.firstOrNull { it.businessType == businessType }
                if (firstProduct != null) {
                    add(
                        LineItemFormState(
                            productId = firstProduct.id,
                            productName = firstProduct.name,
                            quantityText = "1",
                            unitPriceText = firstProduct.sellingPrice.toString(),
                            unitCost = firstProduct.unitCost
                        )
                    )
                } else {
                    add(
                        LineItemFormState(
                            productId = 0L,
                            productName = "Custom Item",
                            quantityText = "1",
                            unitPriceText = "100",
                            unitCost = 50.0
                        )
                    )
                }
            }
        }
    }

    // Filter available products for current business
    val filteredProducts = remember(businessType, availableProducts) {
        availableProducts.filter { it.businessType == businessType }
    }

    // Customer name auto-suggestions
    val filteredCustomerSuggestions = remember(customerName, existingCustomers) {
        if (customerName.isBlank()) emptyList()
        else existingCustomers.filter { it.name.contains(customerName, ignoreCase = true) }
    }

    // Real-time calculation state
    val calculationResult by remember {
        derivedStateOf {
            val items = formLineItems.map { line ->
                val qty = line.quantityText.toIntOrNull() ?: 0
                val price = line.unitPriceText.toDoubleOrNull() ?: 0.0
                LineItemInput(qty, price, line.unitCost)
            }
            val discountVal = discountValueText.toDoubleOrNull() ?: 0.0
            val paidVal = initialPaymentText.toDoubleOrNull() ?: 0.0

            SalesCalculator.calculate(
                businessType = businessType,
                items = items,
                discountType = discountType,
                discountValue = discountVal,
                amountPaid = paidVal,
                currentStatus = statusOverride ?: PaymentStatus.PENDING,
                sintraMarginPercent = sintraMarginPercent,
                sintraUseCostCalculation = sintraUseCostCalculation
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing)
                .widthIn(max = 640.dp)
                .fillMaxWidth(0.96f)
                .heightIn(max = 680.dp)
                .testTag("transaction_dialog"),
            shape = RoundedCornerShape(DialogDefaults.CornerRadius),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, com.amacezing.sales.ui.theme.AppBorder),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when {
                                isDuplicateMode -> "Duplicate Order"
                                isEditMode -> "Edit Order ${initialTransaction?.transaction?.orderNumber}"
                                else -> "New Sales Order"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Fill order details, product lines, and payment",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Form Body
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Business Selection
                    item {
                        Text("Business", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BusinessType.values().forEach { bType ->
                                FilterChip(
                                    selected = businessType == bType,
                                    onClick = {
                                        if (businessType != bType) {
                                            businessType = bType
                                            // Reset first line item to match business catalog
                                            val p = availableProducts.firstOrNull { it.businessType == bType }
                                            if (p != null) {
                                                formLineItems.clear()
                                                formLineItems.add(
                                                    LineItemFormState(
                                                        productId = p.id,
                                                        productName = p.name,
                                                        quantityText = "1",
                                                        unitPriceText = p.sellingPrice.toString(),
                                                        unitCost = p.unitCost
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    label = { Text(bType.displayName) },
                                    modifier = Modifier.weight(1f).testTag("select_business_${bType.name.lowercase()}")
                                )
                            }
                        }
                    }

                    // 2. Customer Name & Date
                    item {
                        Column {
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = {
                                    customerName = it
                                    customerSuggestionsExpanded = it.isNotBlank()
                                    validationError = null
                                },
                                label = { Text("Customer Name *") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("order_customer_name_input")
                            )

                            // Quick suggestion chips
                            if (filteredCustomerSuggestions.isNotEmpty() && customerSuggestionsExpanded) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    filteredCustomerSuggestions.take(3).forEach { cust ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.clickable {
                                                customerName = cust.name
                                                customerSuggestionsExpanded = false
                                            }
                                        ) {
                                            Text(
                                                text = cust.name,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Order Date
                    item {
                        OutlinedTextField(
                            value = Formatters.formatEpochMillisToDate(orderDateMillis),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Order Date") },
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true }
                                .testTag("order_date_picker_field")
                        )
                    }

                    // 3. Product Line Items Section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Product Line Items (${formLineItems.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            TextButton(
                                onClick = {
                                    // Prefer a product that isn't on the order yet.
                                    val usedIds = formLineItems.map { it.productId }.toSet()
                                    val defProd = filteredProducts.firstOrNull { it.id !in usedIds }
                                        ?: filteredProducts.firstOrNull()
                                    val existing = defProd?.let { dp -> formLineItems.firstOrNull { it.productId == dp.id } }
                                    if (existing != null) {
                                        // Every catalog product is already on the order: add to its quantity.
                                        existing.quantityText = ((existing.quantityText.toIntOrNull() ?: 0) + 1).toString()
                                    } else {
                                        formLineItems.add(
                                            LineItemFormState(
                                                productId = defProd?.id ?: 0L,
                                                productName = defProd?.name ?: "Custom Product",
                                                quantityText = "1",
                                                unitPriceText = (defProd?.sellingPrice ?: 50.0).toString(),
                                                unitCost = defProd?.unitCost ?: 25.0
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("add_product_line_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Item", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Product")
                            }
                        }
                    }

                    // Line Items List
                    itemsIndexed(formLineItems) { index, line ->
                        var productDropdownExpanded by remember { mutableStateOf(false) }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            tonalElevation = 1.dp
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Product Selector Dropdown
                                    ExposedDropdownMenuBox(
                                        expanded = productDropdownExpanded,
                                        onExpandedChange = { productDropdownExpanded = it },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = line.productName,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Product #${index + 1}") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = productDropdownExpanded,
                                            onDismissRequest = { productDropdownExpanded = false }
                                        ) {
                                            filteredProducts.forEach { p ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(p.name, fontWeight = FontWeight.Medium)
                                                            Text(
                                                                "Price: ${Formatters.formatCurrency(p.sellingPrice)} • Cost: ${Formatters.formatCurrency(p.unitCost)}",
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        productDropdownExpanded = false
                                                        // Same product (at the same price) already on the order? Merge into it.
                                                        val dup = formLineItems.firstOrNull {
                                                            it !== line && it.productId == p.id &&
                                                                it.unitPriceText == p.sellingPrice.toString()
                                                        }
                                                        if (dup != null) {
                                                            val total = (dup.quantityText.toIntOrNull() ?: 0) +
                                                                (line.quantityText.toIntOrNull() ?: 0)
                                                            dup.quantityText = total.toString()
                                                            formLineItems.remove(line)
                                                        } else {
                                                            line.productId = p.id
                                                            line.productName = p.name
                                                            line.unitPriceText = p.sellingPrice.toString()
                                                            line.unitCost = p.unitCost
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    if (formLineItems.size > 1) {
                                        IconButton(
                                            onClick = { formLineItems.removeAt(index) },
                                            modifier = Modifier.padding(start = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Remove Item",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = line.quantityText,
                                        onValueChange = { line.quantityText = it },
                                        label = { Text("Qty") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(0.8f).testTag("line_qty_input_$index")
                                    )

                                    OutlinedTextField(
                                        value = line.unitPriceText,
                                        onValueChange = { line.unitPriceText = it },
                                        label = { Text("Unit Price (₱)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1.2f).testTag("line_price_input_$index")
                                    )

                                    val lineQty = line.quantityText.toIntOrNull() ?: 0
                                    val linePrice = line.unitPriceText.toDoubleOrNull() ?: 0.0
                                    val subtotal = lineQty * linePrice

                                    Column(
                                        modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text("Subtotal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(Formatters.formatCurrency(subtotal), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 4. Discount Section
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Discount", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Row {
                                    FilterChip(
                                        selected = discountType == DiscountType.PERCENTAGE,
                                        onClick = { discountType = DiscountType.PERCENTAGE },
                                        label = { Text("%") }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    FilterChip(
                                        selected = discountType == DiscountType.FIXED,
                                        onClick = { discountType = DiscountType.FIXED },
                                        label = { Text("₱ Fixed") }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = discountValueText,
                                onValueChange = { discountValueText = it },
                                label = {
                                    Text(if (discountType == DiscountType.PERCENTAGE) "Discount Percentage (%)" else "Fixed Discount Amount (₱)")
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth().testTag("discount_value_input")
                            )
                        }
                    }

                    // 5. Payment & Status Section
                    item {
                        Column {
                            Text("Payment & Remarks", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))

                            if (!isEditMode) {
                                OutlinedTextField(
                                    value = initialPaymentText,
                                    onValueChange = { initialPaymentText = it },
                                    label = { Text("Initial Payment Received (₱)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth().testTag("initial_payment_input")
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            } else {
                                // If editing, allow status override (e.g. Cancelled)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PaymentStatus.values().forEach { st ->
                                        FilterChip(
                                            selected = statusOverride == st,
                                            onClick = { statusOverride = st },
                                            label = { Text(st.displayName) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            OutlinedTextField(
                                value = remarks,
                                onValueChange = { remarks = it },
                                label = { Text("Order Remarks / Payment Notes") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth().testTag("order_remarks_input")
                            )
                        }
                    }

                    // 6. Live Totals Summary Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Gross Sales:", fontSize = 12.sp)
                                    Text(Formatters.formatCurrency(calculationResult.grossSales), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                                if (calculationResult.discountAmount > 0.0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Discount Applied:", fontSize = 12.sp, color = SecondaryAmber)
                                        Text("-${Formatters.formatCurrency(calculationResult.discountAmount)}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SecondaryAmber)
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Net Sales:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(Formatters.formatCurrency(calculationResult.netSales), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Estimated Profit (${if (isProfitMasked) "••••" else Formatters.formatPercent(calculationResult.estimatedMarginPercent)}):", fontSize = 12.sp)
                                    Text(calculationResult.estimatedProfit.formatCurrency(isProfitMasked), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SecondaryAmber)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Outstanding Balance:", fontSize = 12.sp)
                                    Text(
                                        Formatters.formatCurrency(calculationResult.outstandingBalance),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (calculationResult.outstandingBalance > 0.0) StatusPending else StatusPaid
                                    )
                                }
                            }
                        }
                    }

                    if (validationError != null) {
                        item {
                            Text(
                                text = validationError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_order_button")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (customerName.isBlank()) {
                                validationError = "Customer name is required."
                                return@Button
                            }
                            val items = formLineItems.mapNotNull { line ->
                                val qty = line.quantityText.toIntOrNull() ?: 0
                                val price = line.unitPriceText.toDoubleOrNull() ?: 0.0
                                if (qty > 0 && price >= 0.0) {
                                    LineItemInput(qty, price, line.unitCost)
                                } else null
                            }
                            if (items.isEmpty()) {
                                validationError = "At least one product item with quantity > 0 is required."
                                return@Button
                            }

                            val productInfos = formLineItems.map { Pair(it.productId, it.productName) }
                            val discVal = discountValueText.toDoubleOrNull() ?: 0.0
                            val initialPaid = initialPaymentText.toDoubleOrNull() ?: 0.0

                            if (isEditMode && initialTransaction != null) {
                                onSaveEdit(
                                    initialTransaction,
                                    items,
                                    productInfos,
                                    discountType,
                                    discVal,
                                    remarks.trim(),
                                    statusOverride
                                )
                            } else {
                                onSaveNew(
                                    businessType,
                                    customerName.trim(),
                                    orderDateMillis,
                                    items,
                                    productInfos,
                                    discountType,
                                    discVal,
                                    initialPaid,
                                    paymentMethod,
                                    "",
                                    remarks.trim()
                                )
                            }
                        },
                        modifier = Modifier.testTag("save_order_button")
                    ) {
                        Text(if (isEditMode) "Save Changes" else "Create Order")
                    }
                }
            }
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = orderDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { orderDateMillis = it }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
