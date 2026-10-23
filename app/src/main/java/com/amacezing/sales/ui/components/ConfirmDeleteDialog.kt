package com.amacezing.sales.ui.components

import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/** Compact destructive confirmation. Nothing is deleted until [onConfirm] is invoked. */
@Composable
fun ConfirmDeleteDialog(
    title: String = "Confirm Deletion",
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(DialogDefaults.CornerRadius),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = {
            Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            DestructiveActionButton(
                label = "Delete",
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_delete_button")
            )
        },
        dismissButton = {
            DismissActionButton(
                label = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_button")
            )
        }
    )
}

/**
 * Confirms that a copy of an order will be created. The existing duplication logic runs
 * unchanged in [onConfirm]; the original order is never modified.
 */
@Composable
fun DuplicateConfirmDialog(
    orderNumber: String,
    customerName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(DialogDefaults.CornerRadius),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = {
            Text("Duplicate Order?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Text(
                text = "A new order will be created as a copy of $orderNumber for $customerName, " +
                    "using today's date, the same products and discount, and no payments. " +
                    "The original order is not changed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            PrimaryActionButton(
                label = "Duplicate",
                icon = Icons.Default.ContentCopy,
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_duplicate_button")
            )
        },
        dismissButton = {
            DismissActionButton(
                label = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_duplicate_button")
            )
        }
    )
}
