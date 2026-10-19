package com.amacezing.sales.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    TRANSACTIONS("Sales", Icons.Default.ReceiptLong),
    CUSTOMERS("Customers", Icons.Default.People),
    PRODUCTS("Products", Icons.Default.Inventory),
    REPORTS("Reports", Icons.Default.Analytics),
    IMPORT_EXPORT("Excel Migration", Icons.Default.FileUpload),
    SETTINGS("Settings", Icons.Default.Settings),
    PROFILE("Business Profile", Icons.Default.Storefront)
}
