package com.amacezing.sales.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amacezing.sales.model.BusinessType
import com.amacezing.sales.ui.components.BusinessBadge
import com.amacezing.sales.ui.theme.EngravingBrown
import com.amacezing.sales.ui.theme.SecondaryAmber
import com.amacezing.sales.ui.theme.SintraBoardBlue
import com.amacezing.sales.ui.theme.StatusCancelled
import com.amacezing.sales.util.Formatters
import com.amacezing.sales.viewmodel.SalesViewModel

@Composable
fun ProductsScreen(
    viewModel: SalesViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.allProducts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var selectedTab by remember { mutableStateOf<BusinessType?>(null) }

    val filteredProducts = remember(products, selectedTab) {
        if (selectedTab == null) products
        else products.filter { it.businessType == selectedTab }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Products & Pricing",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Manage catalog, unit selling prices, and unit costs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Info Card: Historical Price Isolation
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Historical isolation active: Editing prices here affects future orders only. Past orders preserve their snapshot selling prices and costs.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Business Filter Tabs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTab == null,
                        onClick = { selectedTab = null },
                        label = { Text("All Products (${products.size})") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedTab == BusinessType.ENGRAVING,
                        onClick = { selectedTab = BusinessType.ENGRAVING },
                        label = { Text("Engraving") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedTab == BusinessType.SINTRA_BOARD,
                        onClick = { selectedTab = BusinessType.SINTRA_BOARD },
                        label = { Text("Sintra Board") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Sintra Board Configuration Notice
            if (selectedTab == BusinessType.SINTRA_BOARD || selectedTab == null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SintraBoardBlue.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Sintra Board Profit Calculation",
                                fontWeight = FontWeight.Bold,
                                color = SintraBoardBlue,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (settings.sintraUseCostCalculation)
                                    "Currently using actual unit costs to compute profit."
                                else
                                    "Currently using default margin of ${Formatters.formatPercent(settings.sintraDefaultMarginPercent)} of net sales (from Excel workbook). You can switch this in Settings.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Products List
            if (filteredProducts.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No products in this category.", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(onClick = { viewModel.showProductDialog.value = true }) {
                                Text("Add Product")
                            }
                        }
                    }
                }
            } else {
                items(filteredProducts) { product ->
                    val unitProfit = product.sellingPrice - product.unitCost
                    val unitMargin = if (product.sellingPrice > 0.0) (unitProfit / product.sellingPrice) * 100.0 else 0.0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_item_${product.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Inventory,
                                        contentDescription = null,
                                        tint = if (product.businessType == BusinessType.ENGRAVING) EngravingBrown else SintraBoardBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BusinessBadge(product.businessType)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            viewModel.editingProduct.value = product
                                            viewModel.showProductDialog.value = true
                                        },
                                        modifier = Modifier.size(32.dp).testTag("edit_product_${product.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.itemToDelete.value = product },
                                        modifier = Modifier.size(32.dp).testTag("delete_product_${product.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusCancelled, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Pricing & Cost Details
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Selling Price", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        Formatters.formatCurrency(product.sellingPrice),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column {
                                    Text("Unit Cost", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        Formatters.formatCurrency(product.unitCost),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Estimated Unit Margin", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${Formatters.formatCurrency(unitProfit)} (${Formatters.formatPercent(unitMargin)})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = SecondaryAmber
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { viewModel.showProductDialog.value = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_product_button"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Product")
        }
    }
}
