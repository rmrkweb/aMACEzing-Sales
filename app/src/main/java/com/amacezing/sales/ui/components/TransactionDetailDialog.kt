package com.amacezing.sales.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amacezing.sales.model.PaymentStatus
import com.amacezing.sales.model.TransactionWithDetails
import com.amacezing.sales.ui.theme.AppBorder
import com.amacezing.sales.ui.theme.SecondaryAmber
import com.amacezing.sales.ui.theme.StatusCancelled
import com.amacezing.sales.ui.theme.StatusPaid
import com.amacezing.sales.ui.theme.StatusPending
import com.amacezing.sales.util.Formatters
import com.amacezing.sales.util.formatCurrency

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionDetailDialog(
    transactionWithDetails: TransactionWithDetails,
    isProfitMasked: Boolean = false,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onRecordPayment: () -> Unit,
    onGenerateReceipt: () -> Unit = {},
    onDelete: () -> Unit
) {
    val tx = transactionWithDetails.transaction
    var showDuplicateConfirm by remember { mutableStateOf(false) }
    val canPay = tx.outstandingBalance > 0.0 && tx.paymentStatus != PaymentStatus.CANCELLED

    AppDialog(
        onDismiss = onDismiss,
        modifier = Modifier.testTag("transaction_detail_dialog"),
        header = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.orderNumber,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BusinessBadge(tx.businessType)
                        PaymentStatusBadge(tx.paymentStatus)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = Formatters.formatEpochMillisToDate(tx.orderDateEpochMillis),
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
                PrimaryActionButton(
                    label = "Receipt",
                    icon = Icons.Default.PictureAsPdf,
                    onClick = onGenerateReceipt,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("generate_receipt_pdf_button")
                )
                SecondaryActionButton(
                    label = "Duplicate",
                    icon = Icons.Default.ContentCopy,
                    onClick = { showDuplicateConfirm = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("duplicate_order_button")
                )
            }
            ActionRow {
                if (canPay) {
                    PrimaryActionButton(
                        label = "Pay",
                        icon = Icons.Default.Payment,
                        onClick = onRecordPayment,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("record_payment_action_button")
                    )
                }
                SecondaryActionButton(
                    label = "Edit",
                    icon = Icons.Default.Edit,
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_order_action_button")
                )
                OutlinedIconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(DialogDefaults.ButtonHeight)
                        .testTag("delete_order_action_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StatusCancelled.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete order", tint = StatusCancelled)
                }
            }
        }
    ) {
        // Customer
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionLabel("Customer")
            Text(
                text = tx.customerName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Products
        Column {
            SectionLabel("Products Ordered (${transactionWithDetails.lineItems.size})")
            Spacer(Modifier.height(6.dp))
            transactionWithDetails.lineItems.forEachIndexed { index, line ->
                if (index > 0) HorizontalDivider(color = AppBorder.copy(alpha = 0.6f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(line.productName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "${line.quantity} × ${Formatters.formatCurrency(line.unitSellingPrice)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        Formatters.formatCurrency(line.lineSubtotal),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Financial summary
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, AppBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryRow("Gross Sales", Formatters.formatCurrency(tx.grossSales))

                if (tx.discountAmount > 0.0) {
                    val discLabel = if (tx.discountPercentage > 0.0) {
                        "Discount (${Formatters.formatPercent(tx.discountPercentage)})"
                    } else "Discount"
                    SummaryRow(
                        label = discLabel,
                        value = "-${Formatters.formatCurrency(tx.discountAmount)}",
                        labelColor = SecondaryAmber,
                        valueColor = SecondaryAmber
                    )
                }

                SummaryRow(
                    label = "Net Sales",
                    value = Formatters.formatCurrency(tx.netSales),
                    valueColor = MaterialTheme.colorScheme.primary,
                    emphasized = true
                )

                val margin = if (tx.netSales > 0.0) (tx.estimatedProfit / tx.netSales) * 100.0 else 0.0
                SummaryRow(
                    label = "Estimated Profit (${if (isProfitMasked) "••••" else Formatters.formatPercent(margin)})",
                    value = tx.estimatedProfit.formatCurrency(isProfitMasked)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = AppBorder)

                SummaryRow(
                    label = "Amount Paid",
                    value = Formatters.formatCurrency(tx.amountPaid),
                    valueColor = StatusPaid
                )
                SummaryRow(
                    label = "Outstanding Balance",
                    value = Formatters.formatCurrency(tx.outstandingBalance),
                    valueColor = if (tx.outstandingBalance > 0.0) StatusPending else StatusPaid,
                    emphasized = true
                )
            }
        }

        // Payment history
        if (transactionWithDetails.payments.isNotEmpty()) {
            Column {
                SectionLabel("Recorded Payments (${transactionWithDetails.payments.size})")
                Spacer(Modifier.height(6.dp))
                transactionWithDetails.payments.forEachIndexed { index, pay ->
                    if (index > 0) HorizontalDivider(color = AppBorder.copy(alpha = 0.6f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${pay.paymentMethod}${if (pay.referenceNumber.isNotBlank()) " • Ref: ${pay.referenceNumber}" else ""}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                Formatters.formatEpochMillisToDate(pay.paymentDateEpochMillis),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            Formatters.formatCurrency(pay.amount),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StatusPaid,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Remarks (omitted entirely when empty, so no blank section is left behind)
        if (tx.remarks.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionLabel("Remarks & Notes")
                Text(
                    text = tx.remarks,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDuplicateConfirm) {
        DuplicateConfirmDialog(
            orderNumber = tx.orderNumber,
            customerName = tx.customerName,
            onConfirm = {
                showDuplicateConfirm = false
                onDuplicate()
            },
            onDismiss = { showDuplicateConfirm = false }
        )
    }
}
