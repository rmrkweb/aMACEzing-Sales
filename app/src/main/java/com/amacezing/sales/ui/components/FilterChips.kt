package com.amacezing.sales.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amacezing.sales.ui.theme.AppBackground
import com.amacezing.sales.ui.theme.AppBorder
import com.amacezing.sales.ui.theme.AppTextSecondary

/** Small uppercase caption above a group of filters (e.g. BUSINESS, PERIOD). */
@Composable
fun FilterSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppTextSecondary
    )
}

/**
 * Compact, single-line chip used by the dashboard filters.
 * Selected: filled with [selectedColor] and dark content. Unselected: transparent with a subtle border.
 */
@Composable
fun CompactFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    height: Dp = 40.dp
) {
    val shape = RoundedCornerShape(12.dp)
    val contentColor = if (selected) AppBackground else AppTextSecondary

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = height)
            .clip(shape)
            .background(if (selected) selectedColor else Color.Transparent)
            .border(1.dp, if (selected) selectedColor else AppBorder, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}
