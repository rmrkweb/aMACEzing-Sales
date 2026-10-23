package com.amacezing.sales.ui.components

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.CircleShape
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
    val ButtonHeight = 40.dp
    val MaxWidth = 560.dp
    val ScreenMargin = 16.dp
}

/**
 * Window wrapper for every custom dialog. Keeps the dialog inside the safe area (status bar,
 * display cutout, keyboard and the phone's navigation bar) so a tall dialog is capped to the
 * visible height and its bottom buttons never slide under the system navigation bar.
 * The window is edge-to-edge so the insets are actually reported; if a device still reports no
 * navigation-bar inset, a fixed guard keeps the bottom clear anyway.
 */
@Composable
fun DialogWindow(
    onDismiss: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val navBottom = WindowInsets.navigationBars.getBottom(LocalDensity.current)
        val bottomGuard = if (navBottom == 0) 40.dp else 0.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(
                    start = DialogDefaults.ScreenMargin,
                    end = DialogDefaults.ScreenMargin,
                    top = 24.dp,
                    bottom = 24.dp + bottomGuard
                ),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
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
    DialogWindow(onDismiss = onDismiss) {
        Box(
            modifier = Modifier.fillMaxWidth(),
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

/**
 * The single dialog action style: the shared gray pill ([PillButton]), icon + label, slightly
 * larger than card actions. Buttons below only differ by content color (cyan, red, muted).
 */
@Composable
private fun DialogPillButton(
    label: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    contentColor: Color,
    modifier: Modifier,
    enabled: Boolean,
    compact: Boolean = false
) {
    PillButton(
        label = label,
        icon = icon,
        onClick = onClick,
        contentColor = contentColor,
        modifier = modifier,
        enabled = enabled,
        height = if (compact) 32.dp else DialogDefaults.ButtonHeight,
        horizontalPadding = if (compact) 10.dp else 12.dp,
        iconSize = if (compact) 14.dp else 16.dp,
        fontSize = if (compact) 12.sp else 13.sp
    )
}

/** Main action (Save, Pay, Receipt...). Gray pill, cyan icon + label. */
@Composable
fun PrimaryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    compact: Boolean = false
) = DialogPillButton(label, icon, onClick, MaterialTheme.colorScheme.primary, modifier, enabled, compact)

/** Secondary action (Edit, Duplicate, Share...). Same pill as [PrimaryActionButton]. */
@Composable
fun SecondaryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    compact: Boolean = false
) = DialogPillButton(label, icon, onClick, MaterialTheme.colorScheme.primary, modifier, enabled, compact)

/** Destructive action (Delete, Discard...). Gray pill, red icon + label. */
@Composable
fun DestructiveActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Default.Delete,
    enabled: Boolean = true,
    compact: Boolean = false
) = DialogPillButton(label, icon, onClick, StatusCancelled, modifier, enabled, compact)

/** Dismiss/cancel action. Gray pill, muted icon + label. */
@Composable
fun DismissActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Default.Close,
    enabled: Boolean = true
) = DialogPillButton(label, icon, onClick, MaterialTheme.colorScheme.onSurfaceVariant, modifier, enabled)

/** Equal-width row of actions. */
@Composable
fun ActionRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
