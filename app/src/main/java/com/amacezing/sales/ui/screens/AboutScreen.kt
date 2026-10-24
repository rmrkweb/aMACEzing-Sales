package com.amacezing.sales.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import com.amacezing.sales.ui.navigation.Screen
import com.amacezing.sales.ui.components.PrimaryActionButton
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.PackageInfoCompat
import com.amacezing.sales.ui.theme.AppBorder
import com.amacezing.sales.viewmodel.SalesViewModel
import com.amacezing.sales.viewmodel.SyncStatus
import com.amacezing.sales.update.UpdateSettingsCard

@Composable
fun AboutScreen(
    viewModel: SalesViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val syncStatus by viewModel.syncStatus.collectAsState()
    val versionText = remember {
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val name = info.versionName ?: "—"
            "$name (build ${PackageInfoCompat.getLongVersionCode(info)})"
        } catch (_: Exception) { "—" }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("about_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("About & Maintenance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("App information, cloud sync and updates", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("aMACEzing Sales", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text("Manage Your Business. Grow Your Sales", fontSize = 13.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Card(
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, AppBorder)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                AboutRow("App name", "aMACEzing Sales")
                HorizontalDivider(color = AppBorder)
                AboutRow("Version", versionText, modifier = Modifier.testTag("about_version"))
                HorizontalDivider(color = AppBorder)
                AboutRow("Developer", "Reymark Aguila")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().testTag("about_coffee_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, AppBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Icon(
                            Icons.Default.Coffee,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp).size(28.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Like this app?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Coffee fuels great apps ♡", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                PrimaryActionButton(
                    label = "Buy me a coffee",
                    icon = Icons.Default.Coffee,
                    onClick = { viewModel.navigateTo(Screen.COFFEE) },
                    modifier = Modifier.fillMaxWidth().testTag("about_coffee_button")
                )
            }
        }

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(1.dp, AppBorder), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cloud synchronization", fontWeight = FontWeight.Bold)
                        Text("Status: ${syncStatus.label}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (syncStatus == SyncStatus.SYNCING) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
                PrimaryActionButton(
                    label = if (syncStatus == SyncStatus.SYNCING) "Syncing…" else "Sync to Cloud",
                    icon = Icons.Default.CloudSync,
                    enabled = syncStatus != SyncStatus.SYNCING,
                    onClick = { viewModel.syncAllToCloud() },
                    modifier = Modifier.fillMaxWidth().testTag("about_sync_button")
                )
            }
        }

        UpdateSettingsCard(modifier = Modifier.fillMaxWidth())

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(1.dp, AppBorder), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://rmrkweb.github.io")))
                }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Developer website", fontWeight = FontWeight.SemiBold)
                    Text("https://rmrkweb.github.io", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun AboutRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}
