package com.amacezing.sales.ui.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.amacezing.sales.model.Business
import com.amacezing.sales.ui.theme.AppTextSecondary

/** The current list of businesses, provided once at the app root so any screen can resolve names/colours. */
val LocalBusinesses = compositionLocalOf<List<Business>> { Business.DEFAULTS }

/** Accent colours offered when creating a business (hex strings, stored on the Business row). */
val BusinessColorPalette = listOf(
    "#00B8F0", "#A78BFA", "#22C55E", "#F59E0B", "#EF4444", "#EC4899", "#14B8A6", "#F97316"
)

fun parseBusinessColor(hex: String): Color {
    val cleaned = hex.trim().removePrefix("#")
    val value = cleaned.toLongOrNull(16) ?: return AppTextSecondary
    return if (cleaned.length == 6) Color(0xFF000000L or value) else AppTextSecondary
}

fun List<Business>.findById(id: String): Business? = firstOrNull { it.id == id }

fun List<Business>.nameOf(id: String): String = findById(id)?.name ?: Business.fallbackName(id)

fun List<Business>.colorOf(id: String): Color =
    findById(id)?.let { parseBusinessColor(it.colorHex) } ?: AppTextSecondary

/** Businesses that can be chosen for new records (archived ones stay visible on old records only). */
fun List<Business>.selectable(): List<Business> = filter { it.isActive }.ifEmpty { this }
