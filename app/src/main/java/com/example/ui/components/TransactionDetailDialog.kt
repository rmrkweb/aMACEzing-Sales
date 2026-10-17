package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.PaymentStatus
import com.example.model.TransactionWithDetails
import com.example.ui.theme.SecondaryAmber
import com.example.ui.theme.StatusCancelled
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPending
import com.example.util.Formatters

@Composable
fun TransactionDetailDialog(
    transactionWithDetails: TransactionWithDetails,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onRecordPayment: () -> Unit,
    onDelete: () -> Unit
) {
    val tx = transactionWithDetails.transaction

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .testTag("transaction_detail_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tx.orderNumber,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BusinessBadge(tx.businessType)
                        }
                        Text(
                            text = Formatters.formatEpochMillisToDate(tx.orderDateEpochMillis),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PaymentStatusBadge(tx.paymentStatus)
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Customer Info
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Customer", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(tx.customerName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }

                    // Product Line Items
                    item {
                        Text("Products Ordered (${transactionWithDetails.lineItems.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            transactionWithDetails.lineItems.forEach { line ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(line.productName, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Text(
                                            "${line.quantity} × ${Formatters.formatCurrency(line.unitSellingPrice)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        Formatters.formatCurrency(line.lineSubtotal),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    // Financial Summary Breakdown
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Gross Sales:", fontSize = 12.sp)
                                    Text(Formatters.formatCurrency(tx.grossSales), fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                }

                                if (tx.discountAmount > 0.0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val discLabel = if (tx.discountPercentage > 0.0) "Discount (${Formatters.formatPercent(tx.discountPercentage)}):" else "Discount:"
                                        Text(discLabel, fontSize = 12.sp, color = SecondaryAmber)
                                        Text("-${Formatters.formatCurrency(tx.discountAmount)}", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = SecondaryAmber)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Net Sales:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(Formatters.formatCurrency(tx.netSales), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val margin = if (tx.netSales > 0.0) (tx.estimatedProfit / tx.netSales) * 100.0 else 0.0
                                    Text("Estimated Profit (${Formatters.formatPercent(margin)}):", fontSize = 12.sp)
                                    Text(Formatters.formatCurrency(tx.estimatedProfit), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SecondaryAmber)
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Amount Paid:", fontSize = 12.sp)
                                    Text(Formatters.formatCurrency(tx.amountPaid), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusPaid)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Outstanding Balance:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        Formatters.formatCurrency(tx.outstandingBalance),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (tx.outstandingBalance > 0.0) StatusPending else StatusPaid
                                    )
                                }
                            }
                        }
                    }

                    // Payment Records History
                    if (transactionWithDetails.payments.isNotEmpty()) {
                        item {
                            Text("Recorded Payments (${transactionWithDetails.payments.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                transactionWithDetails.payments.forEach { pay ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                "${pay.paymentMethod} ${if (pay.referenceNumber.isNotBlank()) "• Ref: ${pay.referenceNumber}" else ""}",
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                Formatters.formatEpochMillisToDate(pay.paymentDateEpochMillis),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            Formatters.formatCurrency(pay.amount),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = StatusPaid
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Remarks
                    if (tx.remarks.isNotBlank()) {
                        item {
                            Column {
                                Text("Remarks & Notes", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tx.remarks,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_order_action_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Order", tint = StatusCancelled)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onDuplicate,
                            modifier = Modifier.testTag("duplicate_order_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Duplicate")
                        }

                        if (tx.outstandingBalance > 0.0 && tx.paymentStatus != PaymentStatus.CANCELLED) {
                            Button(
                                onClick = onRecordPayment,
                                modifier = Modifier.testTag("record_payment_action_button")
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = "Pay", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pay")
                            }
                        }

                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.testTag("edit_order_action_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit")
                        }
                    }
                }
            }
        }
    }
}
