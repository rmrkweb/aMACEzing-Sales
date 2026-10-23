package com.amacezing.sales.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Gray pill background shared by every card action and dialog action (dark and light themes). */
@Composable
fun actionPillContainer(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color(0xFF2C3645) else Color(0xFFE2E8F0)

/**
 * The single action look used app-wide: rounded gray pill, icon + label.
 * Sized like the filter chips by default (32dp tall, tight padding). Built on `clickable`
 * (not Material Button) so it does not reserve an extra 48dp touch-target footprint.
 */
@Composable
fun PillButton(
    label: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    contentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 32.dp,
    horizontalPadding: Dp = 10.dp,
    iconSize: Dp = 16.dp,
    fontSize: TextUnit = 12.sp
) {
    val color = if (enabled) contentColor else contentColor.copy(alpha = 0.38f)
    val container = if (enabled) actionPillContainer() else actionPillContainer().copy(alpha = 0.5f)
    Row(
        modifier = modifier
            .heightIn(min = height)
            .clip(CircleShape)
            .background(container)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(iconSize))
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = label,
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Shared card action bar: a divider, then a row of gray pill actions (icon + label).
 * Give each [CardActionButton] `Modifier.weight(...)` so the actions share the bar width.
 */
@Composable
fun CardActionBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/** One card action pill. Pass [color] for destructive actions (icon and label are tinted together). */
@Composable
fun CardActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    PillButton(
        label = label,
        icon = icon,
        onClick = onClick,
        contentColor = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color,
        modifier = modifier,
        horizontalPadding = 6.dp,
        fontSize = 11.sp
    )
}
