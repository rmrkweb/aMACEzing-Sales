package com.amacezing.sales.ui.screens

import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import com.amacezing.sales.ui.components.CardActionButton
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import com.amacezing.sales.ui.navigation.Screen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.amacezing.sales.ui.theme.SecondaryAmber
import com.amacezing.sales.ui.theme.StatusPaid
import com.amacezing.sales.ui.theme.StatusPending
import com.amacezing.sales.viewmodel.SyncStatus
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amacezing.sales.viewmodel.SalesViewModel

@Composable
fun SettingsScreen(
    viewModel: SalesViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val businesses by viewModel.businesses.collectAsState()
    var editingBusiness by remember { mutableStateOf<com.amacezing.sales.model.Business?>(null) }
    var showBusinessDialog by remember { mutableStateOf(false) }


    if (showBusinessDialog) {
        val current = editingBusiness
        var usage by remember(current?.id) {
            mutableStateOf<com.amacezing.sales.data.SalesRepository.BusinessUsage?>(null)
        }
        androidx.compose.runtime.LaunchedEffect(current?.id) {
            usage = current?.let { viewModel.getBusinessUsage(it.id) }
        }
        com.amacezing.sales.ui.components.BusinessEditDialog(
            initial = current,
            usage = usage,
            onDelete = {
                current?.let { viewModel.deleteBusiness(it) }
                showBusinessDialog = false
                editingBusiness = null
            },
            nextSortOrder = (businesses.maxOfOrNull { it.sortOrder } ?: -1) + 1,
            canArchive = businesses.count { it.isActive && it.id != current?.id } > 0,
            validate = { n, p -> viewModel.validateBusiness(n, p, current?.id) },
            newId = { n -> viewModel.newBusinessId(n) },
            onSave = { saved ->
                viewModel.saveBusiness(saved)
                showBusinessDialog = false
                editingBusiness = null
            },
            onDismiss = {
                showBusinessDialog = false
                editingBusiness = null
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "System Settings",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Manage your businesses, privacy and sync",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                }
            }


            // Businesses (entities)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Businesses", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    "Each business has its own order prefix and profit rule.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { editingBusiness = null; showBusinessDialog = true },
                                modifier = Modifier.testTag("add_business_button")
                            ) { Text("Add") }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        businesses.forEach { business ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editingBusiness = business; showBusinessDialog = true }
                                    .padding(vertical = 10.dp)
                                    .testTag("business_row_${business.id.lowercase()}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(com.amacezing.sales.ui.components.parseBusinessColor(business.colorHex))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        business.name + if (!business.isActive) " (archived)" else "",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "Prefix ${business.orderPrefix} • " +
                                            if (business.profitMethod == com.amacezing.sales.model.ProfitMethod.UNIT_COST) "Unit costs"
                                            else "Margin ${if (settings.isProfitMasked) "••••" else com.amacezing.sales.util.Formatters.formatPercent(business.marginPercent)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                CardActionButton(
                                    label = "Edit",
                                    icon = Icons.Default.Edit,
                                    onClick = { editingBusiness = business; showBusinessDialog = true },
                                    modifier = Modifier.testTag("edit_business_${business.id.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // Profit privacy
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mask Profit Values", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "Hide profit amounts on the dashboard until you turn this off.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.isProfitMasked,
                            onCheckedChange = { masked ->
                                viewModel.updateSettings(settings.copy(isProfitMasked = masked))
                            },
                            modifier = Modifier.testTag("mask_profit_toggle")
                        )
                    }
                }
            }

            

            

            // About
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(com.amacezing.sales.ui.navigation.Screen.ABOUT) }
                        .testTag("about_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("About", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "App version and developer",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }
}
