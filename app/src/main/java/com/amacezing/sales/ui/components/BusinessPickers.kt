package com.amacezing.sales.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amacezing.sales.ui.theme.AppBorder
import com.amacezing.sales.util.ColorUtils

/** Suggested colours plus a custom picker (hue / saturation / brightness sliders and a hex field). */
@Composable
fun BusinessColorPicker(
    colorHex: String,
    onColorChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustom by remember { mutableStateOf(false) }
    val isCustom = BusinessColorPalette.none { it.equals(colorHex, ignoreCase = true) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BusinessColorPalette.forEach { hex ->
                val selected = hex.equals(colorHex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(parseBusinessColor(hex))
                        .clickable { onColorChange(hex) }
                        .semantics { contentDescription = "Colour $hex${if (selected) ", selected" else ""}" },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                }
            }
            // Custom colour swatch
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isCustom) parseBusinessColor(colorHex) else MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, AppBorder, CircleShape)
                    .clickable { showCustom = !showCustom }
                    .semantics { contentDescription = "Pick a custom colour" }
                    .testTag("custom_color_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isCustom) Icons.Default.Check else Icons.Default.Palette,
                    contentDescription = null,
                    tint = if (isCustom) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showCustom) {
            var hsv by remember { mutableStateOf(ColorUtils.hexToHsv(colorHex)) }
            var hexText by remember(colorHex) { mutableStateOf(colorHex.uppercase()) }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(parseBusinessColor(colorHex))
                            .border(1.dp, AppBorder, RoundedCornerShape(10.dp))
                    )
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { raw ->
                            val text = ("#" + raw.removePrefix("#").filter { it.isLetterOrDigit() }.take(6)).uppercase()
                            hexText = text
                            if (ColorUtils.isValidHex(text)) {
                                hsv = ColorUtils.hexToHsv(text)
                                onColorChange(text)
                            }
                        },
                        label = { Text("Hex colour") },
                        singleLine = true,
                        isError = !ColorUtils.isValidHex(hexText),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_color_hex_input")
                    )
                }
                Text("Hue", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = hsv[0],
                    onValueChange = { hsv = floatArrayOf(it, hsv[1], hsv[2]); onColorChange(ColorUtils.hsvToHex(hsv[0], hsv[1], hsv[2])) },
                    valueRange = 0f..360f
                )
                Text("Saturation", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = hsv[1],
                    onValueChange = { hsv = floatArrayOf(hsv[0], it, hsv[2]); onColorChange(ColorUtils.hsvToHex(hsv[0], hsv[1], hsv[2])) },
                    valueRange = 0f..1f
                )
                Text("Brightness", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = hsv[2],
                    onValueChange = { hsv = floatArrayOf(hsv[0], hsv[1], it); onColorChange(ColorUtils.hsvToHex(hsv[0], hsv[1], hsv[2])) },
                    valueRange = 0f..1f
                )
            }
        }
    }
}

/** Grid of selectable business icons, tinted with the business colour when selected. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BusinessIconPicker(
    selectedKey: String?,
    tint: Color,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = selectedKey ?: BusinessIcons.first().first
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BusinessIcons.forEach { (key, icon) ->
            val selected = key == current
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) tint.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) tint else AppBorder),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(key) }
                    .semantics { contentDescription = "Icon $key${if (selected) ", selected" else ""}" },
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (selected) tint else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
