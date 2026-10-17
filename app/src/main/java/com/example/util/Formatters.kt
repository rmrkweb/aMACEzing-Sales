package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {
    val MANILA_ZONE_ID: ZoneId = ZoneId.of("Asia/Manila")

    private val currencyFormat: DecimalFormat = run {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            currencySymbol = "₱"
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        DecimalFormat("₱#,##0.00", symbols)
    }

    private val numberFormat: DecimalFormat = run {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        DecimalFormat("#,##0.00", symbols)
    }

    private val integerFormat: DecimalFormat = run {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }
        DecimalFormat("#,##0", symbols)
    }

    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US)
    val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd", Locale.US)
    val monthYearFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)
    val isoDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    fun formatCurrency(amount: Double): String {
        return currencyFormat.format(amount)
    }

    fun formatNumber(amount: Double): String {
        return numberFormat.format(amount)
    }

    fun formatInteger(amount: Int): String {
        return integerFormat.format(amount)
    }

    fun formatPercent(percent: Double): String {
        val df = DecimalFormat("0.##'%'", DecimalFormatSymbols(Locale.US))
        return df.format(percent)
    }

    fun formatEpochMillisToDate(epochMillis: Long): String {
        val localDate = Instant.ofEpochMilli(epochMillis)
            .atZone(MANILA_ZONE_ID)
            .toLocalDate()
        return localDate.format(dateFormatter)
    }

    fun formatEpochMillisToMonthYear(epochMillis: Long): String {
        val localDate = Instant.ofEpochMilli(epochMillis)
            .atZone(MANILA_ZONE_ID)
            .toLocalDate()
        return localDate.format(monthYearFormatter)
    }

    fun formatEpochMillisToIso(epochMillis: Long): String {
        val localDate = Instant.ofEpochMilli(epochMillis)
            .atZone(MANILA_ZONE_ID)
            .toLocalDate()
        return localDate.format(isoDateFormatter)
    }

    fun parseIsoDateToEpochMillis(dateStr: String): Long? {
        return try {
            val localDate = LocalDate.parse(dateStr, isoDateFormatter)
            localDate.atStartOfDay(MANILA_ZONE_ID).toInstant().toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}
