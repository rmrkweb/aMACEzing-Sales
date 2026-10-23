package com.amacezing.sales.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import com.amacezing.sales.model.Business
import com.amacezing.sales.ui.theme.AppTextSecondary

/** The current list of businesses, provided once at the app root so any screen can resolve names/colours. */
val LocalBusinesses = compositionLocalOf<List<Business>> { emptyList() }

/** Accent colours offered when creating a business (hex strings, stored on the Business row). */
val BusinessColorPalette = listOf(
    "#00B8F0", "#A78BFA", "#22C55E", "#F59E0B", "#EF4444", "#EC4899", "#14B8A6", "#F97316"
)

/** Icons offered for a business. The key is what gets stored; unknown keys fall back to the store icon. */
val BusinessIcons: List<Pair<String, ImageVector>> = listOf(
    "store" to Icons.Default.Store,
    "print" to Icons.Default.Print,
    "handyman" to Icons.Default.Handyman,
    "brush" to Icons.Default.Brush,
    "palette" to Icons.Default.Palette,
    "design" to Icons.Default.DesignServices,
    "cake" to Icons.Default.Cake,
    "celebration" to Icons.Default.Celebration,
    "gift" to Icons.Default.CardGiftcard,
    "shirt" to Icons.Default.Checkroom,
    "build" to Icons.Default.Build,
    "inventory" to Icons.Default.Inventory2,
    "cafe" to Icons.Default.LocalCafe,
    "restaurant" to Icons.Default.Restaurant,
    "bag" to Icons.Default.ShoppingBag,
    "camera" to Icons.Default.CameraAlt,
    "cube" to Icons.Default.ViewInAr,
    "category" to Icons.Default.Category,
    "diamond" to Icons.Default.Diamond,
    "computer" to Icons.Default.Computer,
    "bolt" to Icons.Default.Bolt,
    "spa" to Icons.Default.Spa,
    "pets" to Icons.Default.Pets,
    "flower" to Icons.Default.LocalFlorist,
    "book" to Icons.Default.MenuBook,
    "cut" to Icons.Default.ContentCut
)

fun businessIcon(key: String?): ImageVector =
    BusinessIcons.firstOrNull { it.first == key }?.second ?: Icons.Default.Store

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

fun List<Business>.iconOf(id: String): ImageVector = businessIcon(findById(id)?.iconKey)
