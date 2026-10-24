package com.amacezing.sales.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(val title: String, val icon: ImageVector) {
    // 5 Primary Bottom Navigation Destinations
    DASHBOARD("Home", Icons.Default.Home),
    QUOTATIONS("Orders", Icons.Default.Assignment),
    TRANSACTIONS("Sales", Icons.Default.PointOfSale),
    PRODUCTS("Inventory", Icons.Default.Inventory2),
    PROFILE("Profile", Icons.Default.Person),

    // Secondary Feature Screens (Accessible via Home cards and Profile hub)
    CUSTOMERS("Customers", Icons.Default.People),
    REPORTS("Reports", Icons.Default.Analytics),
    IMPORT_EXPORT("Import & Export", Icons.Default.FileUpload),
    SETTINGS("Settings", Icons.Default.Settings),
    ABOUT("About", Icons.Default.Info),
    COFFEE("Buy me a coffee", Icons.Default.Coffee)
}
