package com.amacezing.sales.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Icon
import com.amacezing.sales.ui.theme.AppBorder
import com.amacezing.sales.ui.theme.StatusCancelled

/** Standard dimensions shared by every redesigned dialog. */
object DialogDefaults {
    val CornerRadius = 22.dp
    val ContentPadding = 20.dp
    val SectionSpacing = 16.dp
    val ButtonHeight = 48.dp
    val MaxWidth = 560.dp
    val ScreenMargin = 16.dp
}

/**
 * Dark, bordered dialog container. The header and the action bar stay pinned while
 * [content] scrolls, so primary actions are never pushed off-screen on small devices.
 * Width is capped for tablets and respects system-bar / display-cutout insets.
 */
@Composable
fun AppDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = DialogDefaults.MaxWidth,
    header: @Composable () -> Unit,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = DialogDefaults.ScreenMargin, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = modifier
                    .widthIn(max = maxWidth)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(DialogDefaults.CornerRadius),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, AppBorder),
                tonalElevation = 0.dp,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(DialogDefaults.ContentPadding)) {
                    header()
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        color = AppBorder
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(top = 8.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(DialogDefaults.SectionSpacing),
                        content = content
                    )
                    if (actions != null) {
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 8.dp, bottom = 14.dp),
                            color = AppBorder
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            content = actions
                        )
                    }
                }
            }
        }
    }
}

/** Small uppercase secondary-colored section label. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** One label/value row; the value is right-aligned and never clipped. */
@Composable
fun SummaryRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    emphasized: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = if (emphasized) 14.sp else 13.sp,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
            color = if (emphasized) MaterialTheme.colorScheme.onSurface else labelColor
        )
        Text(
            text = value,
            fontSize = if (emphasized) 16.sp else 13.sp,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            softWrap = false
        )
    }
}

private val ActionContentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
private val ActionShape = RoundedCornerShape(12.dp)

@Composable
private fun ActionContent(icon: ImageVector?, label: String, iconDescription: String?) {
    if (icon != null) {
        Icon(icon, contentDescription = iconDescription, modifier = Modifier.size(18.dp))
        androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
    }
    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
}

/** Filled cyan action. */
@Composable
fun PrimaryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = DialogDefaults.ButtonHeight),
        shape = ActionShape,
        contentPadding = ActionContentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) { ActionContent(icon, label, null) }
}

/** Outlined action with a subtle border and cyan content. */
@Composable
fun SecondaryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = DialogDefaults.ButtonHeight),
        shape = ActionShape,
        contentPadding = ActionContentPadding,
        border = BorderStroke(1.dp, if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else AppBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
    ) { ActionContent(icon, label, null) }
}

/** Red destructive action (filled). */
@Composable
fun DestructiveActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = DialogDefaults.ButtonHeight),
        shape = ActionShape,
        contentPadding = ActionContentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = StatusCancelled,
            contentColor = Color.White
        )
    ) { Text(label, maxLines = 1, fontWeight = FontWeight.SemiBold) }
}

/** Quiet dismiss/cancel action. */
@Composable
fun DismissActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = DialogDefaults.ButtonHeight),
        shape = ActionShape,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
    ) { Text(label, maxLines = 1, fontWeight = FontWeight.Medium) }
}

/** Equal-width row of actions. */
@Composable
fun ActionRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** Shown instead of a creation dialog when no business exists yet. */
@Composable
fun NoBusinessDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(DialogDefaults.CornerRadius),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = { Text("Add a business first", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
        text = {
            Text(
                "Orders, products and quotations belong to a business. Create your first one in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            PrimaryActionButton(label = "Open Settings", onClick = onOpenSettings)
        },
        dismissButton = { DismissActionButton(label = "Close", onClick = onDismiss) }
    )
}
