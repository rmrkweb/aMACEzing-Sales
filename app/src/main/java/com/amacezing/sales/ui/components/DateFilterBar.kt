package com.amacezing.sales.ui.components

import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.amacezing.sales.ui.theme.AccentCyan
import com.amacezing.sales.ui.theme.AccentViolet
import com.amacezing.sales.ui.theme.AppBackground
import com.amacezing.sales.ui.theme.AppBorder
import com.amacezing.sales.ui.theme.AppSurface
import com.amacezing.sales.ui.theme.AppSurfaceElevated
import com.amacezing.sales.ui.theme.AppTextMuted
import com.amacezing.sales.ui.theme.AppTextPrimary
import com.amacezing.sales.ui.theme.AppTextSecondary
import com.amacezing.sales.ui.theme.StatusCancelled
import com.amacezing.sales.util.Formatters
import com.amacezing.sales.viewmodel.DateRangeType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val monthDay: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
private val monthDayYear: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
private val longDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)

/** "Oct 1 – Oct 21, 2026" / "Dec 15, 2025 – Jan 10, 2026" / "Oct 21, 2026" */
fun formatDateRangeLabel(start: LocalDate, end: LocalDate): String = when {
    start == end -> start.format(monthDayYear)
    start.year == end.year -> "${start.format(monthDay)} – ${end.format(monthDayYear)}"
    else -> "${start.format(monthDayYear)} – ${end.format(monthDayYear)}"
}

private fun millisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(Formatters.MANILA_ZONE_ID).toLocalDate()

// The Material date picker works in UTC-midnight milliseconds.
private fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.pickerMillisToLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/**
 * PERIOD label + compact date chips + active custom-range summary.
 * Selecting "Custom Range" opens a calendar dialog; every other chip is applied immediately.
 */
@Composable
fun DateFilterBar(
    selectedType: DateRangeType,
    customStartMillis: Long?,
    customEndMillis: Long?,
    onSelectType: (DateRangeType) -> Unit,
    onApplyCustomRange: (LocalDate, LocalDate) -> Boolean,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val customStart = customStartMillis?.let { millisToLocalDate(it) }
    val customEnd = customEndMillis?.let { millisToLocalDate(it) }

    Column(modifier = modifier) {
        FilterSectionLabel("Period")
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DateRangeType.values().forEach { range ->
                val isCustom = range == DateRangeType.CUSTOM
                // Same chip as the Sales status filters (default Material FilterChip colors and height).
                FilterChip(
                    selected = selectedType == range,
                    onClick = { if (isCustom) showDialog = true else onSelectType(range) },
                    label = { Text(range.label) },
                    leadingIcon = if (isCustom) {
                        { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    modifier = Modifier.testTag("date_range_${range.name.lowercase()}")
                )
            }
        }

        // Always-visible summary so the active range stays clear even when the chip row is scrolled.
        if (selectedType == DateRangeType.CUSTOM && customStart != null && customEnd != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().testTag("custom_range_summary"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = AccentViolet, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatDateRangeLabel(customStart, customEnd),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showDialog = true }) {
                    Text("Edit", color = AccentViolet, fontSize = 13.sp)
                }
            }
        }
    }

    if (showDialog) {
        DateRangeSelectionDialog(
            initialStart = customStart,
            initialEnd = customEnd,
            onDismiss = { showDialog = false },
            onApply = onApplyCustomRange
        )
    }
}

private data class DatePreset(val label: String, val range: (LocalDate) -> Pair<LocalDate, LocalDate>)

private val presets = listOf(
    DatePreset("Yesterday") { today -> today.minusDays(1) to today.minusDays(1) },
    DatePreset("Last Week") { today ->
        val thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val start = thisMonday.minusWeeks(1)
        start to start.plusDays(6)
    },
    DatePreset("Last Month") { today ->
        val start = today.minusMonths(1).withDayOfMonth(1)
        start to start.with(TemporalAdjusters.lastDayOfMonth())
    },
    DatePreset("This Quarter") { today ->
        val start = LocalDate.of(today.year, ((today.monthValue - 1) / 3) * 3 + 1, 1)
        start to start.plusMonths(3).minusDays(1)
    },
    DatePreset("Last Quarter") { today ->
        val thisQuarterStart = LocalDate.of(today.year, ((today.monthValue - 1) / 3) * 3 + 1, 1)
        val start = thisQuarterStart.minusMonths(3)
        start to thisQuarterStart.minusDays(1)
    },
    DatePreset("Last Year") { today ->
        LocalDate.of(today.year - 1, 1, 1) to LocalDate.of(today.year - 1, 12, 31)
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeSelectionDialog(
    initialStart: LocalDate?,
    initialEnd: LocalDate?,
    onDismiss: () -> Unit,
    onApply: (LocalDate, LocalDate) -> Boolean
) {
    val today = remember { LocalDate.now(Formatters.MANILA_ZONE_ID) }

    var startMillis by remember { mutableStateOf(initialStart?.toPickerMillis()) }
    var endMillis by remember { mutableStateOf(initialEnd?.toPickerMillis()) }
    var pickerVersion by remember { mutableStateOf(0) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val startDate = startMillis?.pickerMillisToLocalDate()
    // Only one date picked = a single-day range.
    val endDate = (endMillis ?: startMillis)?.pickerMillisToLocalDate()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .testTag("date_range_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = AppSurfaceElevated,
            border = BorderStroke(1.dp, AppBorder)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text(
                    text = "Select Date Range",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DateField("Start Date", startDate, Modifier.weight(1f).testTag("range_start_field"))
                    DateField("End Date", endDate, Modifier.weight(1f).testTag("range_end_field"))
                }

                errorText?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = StatusCancelled, fontSize = 12.sp, modifier = Modifier.testTag("range_error"))
                }

                Spacer(modifier = Modifier.height(12.dp))

                FilterSectionLabel("Quick select")
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        val (s, e) = preset.range(today)
                        CompactFilterChip(
                            label = preset.label,
                            selected = startDate == s && endDate == e,
                            onClick = {
                                startMillis = s.toPickerMillis()
                                endMillis = e.toPickerMillis()
                                errorText = null
                                pickerVersion++
                            },
                            selectedColor = AccentViolet,
                            height = 36.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Rebuilt when a preset is chosen so the calendar jumps to the selected month.
                key(pickerVersion) {
                    val pickerState = rememberDateRangePickerState(
                        initialSelectedStartDateMillis = startMillis,
                        initialSelectedEndDateMillis = endMillis,
                        initialDisplayedMonthMillis = startMillis ?: today.toPickerMillis()
                    )
                    LaunchedEffect(pickerState.selectedStartDateMillis, pickerState.selectedEndDateMillis) {
                        startMillis = pickerState.selectedStartDateMillis
                        endMillis = pickerState.selectedEndDateMillis
                        errorText = null
                    }
                    DateRangePicker(
                        state = pickerState,
                        modifier = Modifier.weight(1f),
                        title = null,
                        headline = null,
                        showModeToggle = false,
                        colors = DatePickerDefaults.colors(
                            containerColor = Color.Transparent,
                            weekdayContentColor = AppTextSecondary,
                            subheadContentColor = AppTextSecondary,
                            navigationContentColor = AppTextPrimary,
                            dayContentColor = AppTextPrimary,
                            disabledDayContentColor = AppTextMuted,
                            selectedDayContainerColor = AccentCyan,
                            selectedDayContentColor = AppBackground,
                            todayContentColor = AccentCyan,
                            todayDateBorderColor = AccentCyan,
                            dayInSelectionRangeContainerColor = AccentCyan.copy(alpha = 0.18f),
                            dayInSelectionRangeContentColor = AppTextPrimary,
                            yearContentColor = AppTextPrimary,
                            currentYearContentColor = AccentCyan,
                            selectedYearContainerColor = AccentCyan,
                            selectedYearContentColor = AppBackground
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DismissActionButton(
                        label = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.testTag("range_cancel_button")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    PrimaryActionButton(
                        label = "Apply Range",
                        icon = Icons.Default.Check,
                        onClick = {
                            val s = startDate
                            val e = endDate
                            when {
                                s == null || e == null -> errorText = "Select a start date (and an end date for a range)."
                                s.isAfter(e) -> errorText = "The start date must not be after the end date."
                                onApply(s, e) -> onDismiss()
                                else -> errorText = "That date range can't be applied."
                            }
                        },
                        modifier = Modifier.testTag("range_apply_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun DateField(label: String, date: LocalDate?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = AppSurface,
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, fontSize = 11.sp, color = AppTextSecondary)
            Text(
                text = date?.format(longDate) ?: "Select date",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (date != null) AppTextPrimary else AppTextMuted,
                maxLines = 1
            )
        }
    }
}
