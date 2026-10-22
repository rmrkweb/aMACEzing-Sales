package com.amacezing.sales.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amacezing.sales.model.Business
import com.amacezing.sales.model.ProfitMethod

/**
 * Add or edit a business. [validate] returns an error message (or null) for the name/prefix,
 * [newId] builds a unique id from the name for new businesses, and [canArchive] is false when
 * this is the only active business (at least one must stay selectable).
 */
@Composable
fun BusinessEditDialog(
    initial: Business?,
    nextSortOrder: Int,
    canArchive: Boolean,
    validate: (name: String, prefix: String) -> String?,
    newId: (name: String) -> String,
    onSave: (Business) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var prefix by remember { mutableStateOf(initial?.orderPrefix ?: "") }
    var colorHex by remember { mutableStateOf(initial?.colorHex ?: BusinessColorPalette.first()) }
    var method by remember { mutableStateOf(initial?.profitMethod ?: ProfitMethod.UNIT_COST) }
    var marginText by remember {
        mutableStateOf(
            initial?.marginPercent?.takeIf { it > 0.0 }?.let { "%.2f".format(it).trimEnd('0').trimEnd('.') } ?: ""
        )
    }
    var useCost by remember { mutableStateOf(initial?.useUnitCostIfAvailable ?: false) }
    var active by remember { mutableStateOf(initial?.isActive ?: true) }
    var error by remember { mutableStateOf<String?>(null) }
    var marginError by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val formError = validate(name, prefix)
        if (formError != null) {
            error = formError
            return
        }
        var margin = 0.0
        if (method == ProfitMethod.FIXED_MARGIN) {
            val parsed = marginText.toDoubleOrNull()
            if (parsed == null || parsed < 0.0 || parsed > 100.0) {
                marginError = "Enter a margin between 0 and 100."
                return
            }
            margin = parsed
        }
        onSave(
            Business(
                id = initial?.id ?: newId(name),
                name = name.trim(),
                orderPrefix = prefix.trim().uppercase(),
                colorHex = colorHex,
                profitMethod = method,
                marginPercent = margin,
                useUnitCostIfAvailable = method == ProfitMethod.FIXED_MARGIN && useCost,
                isActive = if (initial == null) true else active,
                sortOrder = initial?.sortOrder ?: nextSortOrder
            )
        )
    }

    AppDialog(
        onDismiss = onDismiss,
        maxWidth = 480.dp,
        modifier = Modifier.testTag("business_edit_dialog"),
        header = {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (initial == null) "Add Business" else "Edit Business",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        actions = {
            ActionRow {
                SecondaryActionButton(
                    label = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                PrimaryActionButton(
                    label = "Save",
                    onClick = ::submit,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_business_button")
                )
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text("Business name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                isError = error != null,
                supportingText = { error?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("business_name_input")
            )
            OutlinedTextField(
                value = prefix,
                onValueChange = { raw ->
                    prefix = raw.filter { it.isLetterOrDigit() }.uppercase().take(6)
                    error = null
                },
                label = { Text("Order number prefix") },
                supportingText = {
                    Text("Orders look like ${prefix.ifBlank { "PREFIX" }}-2026-0001. Changing it only affects new orders.")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("business_prefix_input")
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Accent colour")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BusinessColorPalette.forEach { hex ->
                    val selected = hex.equals(colorHex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(parseBusinessColor(hex))
                            .clickable { colorHex = hex }
                            .semantics { contentDescription = "Colour $hex${if (selected) ", selected" else ""}" },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = androidx.compose.ui.graphics.Color.Black)
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Profit calculation")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfitMethod.entries.forEach { m ->
                    FilterChip(
                        selected = method == m,
                        onClick = { method = m; marginError = null },
                        label = { Text(m.displayName) },
                        modifier = Modifier.testTag("profit_method_${m.name.lowercase()}")
                    )
                }
            }
            Text(
                text = if (method == ProfitMethod.UNIT_COST)
                    "Profit = net sales − product unit costs."
                else
                    "Profit = net sales × margin %. Useful when unit costs aren't tracked.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (method == ProfitMethod.FIXED_MARGIN) {
                OutlinedTextField(
                    value = marginText,
                    onValueChange = { raw ->
                        marginText = raw.filterIndexed { i, c -> c.isDigit() || (c == '.' && raw.indexOf('.') == i) }.take(6)
                        marginError = null
                    },
                    label = { Text("Profit margin (%)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = marginError != null,
                    supportingText = { marginError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("business_margin_input")
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Use actual costs when available", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "If an order has unit costs, use net sales − costs; otherwise use the margin.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useCost,
                        onCheckedChange = { useCost = it },
                        modifier = Modifier.testTag("business_use_cost_toggle")
                    )
                }
            }
        }

        if (initial != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Active", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (canArchive || !initial.isActive)
                            "Archived businesses are hidden when creating new orders, products and quotations. Past records are kept."
                        else
                            "At least one business must stay active.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = active,
                    onCheckedChange = { active = it },
                    enabled = canArchive || !initial.isActive,
                    modifier = Modifier.testTag("business_active_toggle")
                )
            }
        }
    }
}
