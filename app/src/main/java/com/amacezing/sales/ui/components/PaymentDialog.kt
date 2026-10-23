package com.amacezing.sales.ui.components

import androidx.compose.material.icons.filled.Payment
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amacezing.sales.model.TransactionWithDetails
import com.amacezing.sales.ui.theme.AppBorder
import com.amacezing.sales.ui.theme.StatusPending
import com.amacezing.sales.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDialog(
    transactionWithDetails: TransactionWithDetails,
    onRecordPayment: (amount: Double, method: String, reference: String, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    val tx = transactionWithDetails.transaction
    var amountText by remember { mutableStateOf(tx.outstandingBalance.toString()) }
    var paymentMethod by remember { mutableStateOf("GCash") }
    var methodExpanded by remember { mutableStateOf(false) }
    var referenceNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // Guards against double-taps recording the same payment twice.
    var submitted by remember { mutableStateOf(false) }

    val methods = listOf("GCash", "Cash", "Bank Transfer", "Check", "Other")

    fun submit() {
        if (submitted) return
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            errorMessage = "Please enter a valid positive amount."
            return
        }
        if (amount > tx.outstandingBalance + 0.01) {
            errorMessage = "Amount exceeds balance (${Formatters.formatCurrency(tx.outstandingBalance)})."
            return
        }
        submitted = true
        onRecordPayment(amount, paymentMethod, referenceNumber.trim(), notes.trim())
    }

    AppDialog(
        onDismiss = onDismiss,
        maxWidth = 440.dp,
        modifier = Modifier.testTag("payment_dialog"),
        header = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Record Payment",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${tx.orderNumber} • ${tx.customerName}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        actions = {
            ActionRow {
                DismissActionButton(
                    label = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cancel_payment_button")
                )
                PrimaryActionButton(
                    label = "Record Payment",
                    icon = Icons.Default.Payment,
                    onClick = ::submit,
                    enabled = !submitted,
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("submit_payment_button")
                )
            }
        }
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, AppBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                SummaryRow(
                    label = "Outstanding Balance",
                    value = Formatters.formatCurrency(tx.outstandingBalance),
                    valueColor = StatusPending,
                    emphasized = true
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = amountText,
                onValueChange = { raw ->
                    // Digits with at most one decimal point and two decimal places.
                    val cleaned = raw.filterIndexed { i, c ->
                        c.isDigit() || (c == '.' && raw.indexOf('.') == i)
                    }
                    val dot = cleaned.indexOf('.')
                    amountText = if (dot >= 0 && cleaned.length - dot > 3) cleaned.take(dot + 3) else cleaned
                    errorMessage = null
                },
                label = { Text("Amount to pay (₱)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_amount_input"),
                isError = errorMessage != null,
                supportingText = {
                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            )

            ExposedDropdownMenuBox(
                expanded = methodExpanded,
                onExpandedChange = { methodExpanded = it }
            ) {
                OutlinedTextField(
                    value = paymentMethod,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = { Text("Payment method") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("payment_method_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = methodExpanded,
                    onDismissRequest = { methodExpanded = false }
                ) {
                    methods.forEach { method ->
                        DropdownMenuItem(
                            text = { Text(method) },
                            onClick = {
                                paymentMethod = method
                                methodExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = referenceNumber,
                onValueChange = { referenceNumber = it },
                label = { Text("Reference / receipt no. (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_reference_input")
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_notes_input")
            )
        }
    }
}
