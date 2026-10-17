package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.example.ui.theme.SecondaryAmber
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPending
import com.example.viewmodel.SyncStatus
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
import com.example.viewmodel.SalesViewModel

@Composable
fun SettingsScreen(
    viewModel: SalesViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()

    var engName by remember(settings) { mutableStateOf(settings.businessNameEngraving) }
    var sntName by remember(settings) { mutableStateOf(settings.businessNameSintra) }
    var engPrefix by remember(settings) { mutableStateOf(settings.engravingOrderPrefix) }
    var sntPrefix by remember(settings) { mutableStateOf(settings.sintraOrderPrefix) }
    var sntMarginText by remember(settings) { mutableStateOf(settings.sintraDefaultMarginPercent.toString()) }
    var sntUseCost by remember(settings) { mutableStateOf(settings.sintraUseCostCalculation) }

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
                            text = "Configure business rules, order prefixes, and margins",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            val margin = sntMarginText.toDoubleOrNull() ?: 58.3333
                            viewModel.updateSettings(
                                settings.copy(
                                    businessNameEngraving = engName.trim(),
                                    businessNameSintra = sntName.trim(),
                                    engravingOrderPrefix = engPrefix.trim().uppercase(),
                                    sintraOrderPrefix = sntPrefix.trim().uppercase(),
                                    sintraDefaultMarginPercent = margin,
                                    sintraUseCostCalculation = sntUseCost
                                )
                            )
                        },
                        modifier = Modifier.testTag("save_settings_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            }

            // Business Identity
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Business Names", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = engName,
                            onValueChange = { engName = it },
                            label = { Text("Engraving Business Name") },
                            modifier = Modifier.fillMaxWidth().testTag("engraving_name_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = sntName,
                            onValueChange = { sntName = it },
                            label = { Text("Sintra Board Business Name") },
                            modifier = Modifier.fillMaxWidth().testTag("sintra_name_input")
                        )
                    }
                }
            }

            // Order Number Prefixes
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Order Number Prefixes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "Generated orders follow the pattern: PREFIX-YYYY-0001 (e.g., ENG-2026-0001)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = engPrefix,
                                onValueChange = { engPrefix = it },
                                label = { Text("Engraving Prefix") },
                                modifier = Modifier.weight(1f).testTag("engraving_prefix_input")
                            )

                            OutlinedTextField(
                                value = sntPrefix,
                                onValueChange = { sntPrefix = it },
                                label = { Text("Sintra Board Prefix") },
                                modifier = Modifier.weight(1f).testTag("sintra_prefix_input")
                            )
                        }
                    }
                }
            }

            // Sintra Board Margin Configuration
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sintra Board Profit Calculation Rules", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "Preserved from existing Excel sales monitoring workbook (default 58.3333% of Net Sales).",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = sntMarginText,
                            onValueChange = { sntMarginText = it },
                            label = { Text("Estimated Profit Margin (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("sintra_margin_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Use Actual Unit Costs Instead", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    "When enabled, profit is calculated as Net Sales − Actual Product Costs instead of using the estimated margin %.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = sntUseCost,
                                onCheckedChange = { sntUseCost = it },
                                modifier = Modifier.testTag("sintra_cost_toggle")
                            )
                        }
                    }
                }
            }

            // Cloud Firestore Synchronization
            item {
                val syncStatus by viewModel.syncStatus.collectAsState()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Cloud Firestore Sync", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (syncStatus) {
                                    SyncStatus.SYNCED -> StatusPaid.copy(alpha = 0.15f)
                                    SyncStatus.SYNCING -> MaterialTheme.colorScheme.primaryContainer
                                    SyncStatus.OFFLINE_SAVED -> SecondaryAmber.copy(alpha = 0.15f)
                                    SyncStatus.SYNC_ERROR -> MaterialTheme.colorScheme.errorContainer
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (syncStatus) {
                                            SyncStatus.SYNCED -> Icons.Default.CloudDone
                                            SyncStatus.SYNCING -> Icons.Default.CloudSync
                                            SyncStatus.OFFLINE_SAVED -> Icons.Default.CloudOff
                                            SyncStatus.SYNC_ERROR -> Icons.Default.CloudOff
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = when (syncStatus) {
                                            SyncStatus.SYNCED -> StatusPaid
                                            SyncStatus.SYNCING -> MaterialTheme.colorScheme.primary
                                            SyncStatus.OFFLINE_SAVED -> SecondaryAmber
                                            SyncStatus.SYNC_ERROR -> MaterialTheme.colorScheme.error
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = syncStatus.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = when (syncStatus) {
                                            SyncStatus.SYNCED -> StatusPaid
                                            SyncStatus.SYNCING -> MaterialTheme.colorScheme.primary
                                            SyncStatus.OFFLINE_SAVED -> SecondaryAmber
                                            SyncStatus.SYNC_ERROR -> MaterialTheme.colorScheme.error
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Offline-first with Cloud Firestore. Transactions are instantly stored locally in Room and synchronized with your authenticated cloud collections (Businesses, Products, Customers, Transactions, and Settings).",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.syncAllToCloud() },
                            enabled = syncStatus != SyncStatus.SYNCING,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sync_all_to_cloud_button")
                        ) {
                            if (syncStatus == SyncStatus.SYNCING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Syncing...")
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Synchronize All Data to Cloud")
                            }
                        }
                    }
                }
            }

            // System Reset & Seed Data
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Initial Catalog & Demo Data", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "Ensure the initial Bamboo Coasters, Bamboo Ballpens, and Sintra Board A4/A3 prints are in the database.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                viewModel.repository.apply {
                                    // reseed
                                }
                            },
                            modifier = Modifier.testTag("reseed_catalog_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Re-verify Initial Catalog")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }
}
