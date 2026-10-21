package com.amacezing.sales.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.amacezing.sales.ui.theme.TechAccentCyan
import com.amacezing.sales.ui.theme.TechBorderSubtle
import com.amacezing.sales.ui.theme.TechSurfaceDeepNavy
import com.amacezing.sales.ui.theme.TechTextPrimary
import com.amacezing.sales.ui.theme.TechTextSilver

private enum class LogoutStage { IDLE, CONFIRM, WORKING, SYNC_FAILED }

/** Profile hub: quick links to screens that have no bottom-nav tab. */
@Composable
fun ProfileShortcutsCard(
    onOpenReports: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenCustomers: () -> Unit,
    syncBeforeLogout: suspend () -> Boolean,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var stage by remember { mutableStateOf(LogoutStage.IDLE) }
    val scope = rememberCoroutineScope()

    when (stage) {
        LogoutStage.IDLE -> Unit

        LogoutStage.CONFIRM -> AlertDialog(
            onDismissRequest = { stage = LogoutStage.IDLE },
            title = { Text("Log out?") },
            text = {
                Text(
                    "Your data is synced to the cloud first, then removed from this device. " +
                        "You'll be returned to Google sign-in, and your records are restored when you sign back in."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        stage = LogoutStage.WORKING
                        scope.launch {
                            if (syncBeforeLogout()) {
                                stage = LogoutStage.IDLE
                                onLogout()
                            } else {
                                stage = LogoutStage.SYNC_FAILED
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_logout_button")
                ) { Text("Log out", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { stage = LogoutStage.IDLE }) { Text("Cancel") } }
        )

        LogoutStage.WORKING -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Backing up your data…") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Syncing to the cloud before logging out.")
                }
            },
            confirmButton = {}
        )

        LogoutStage.SYNC_FAILED -> AlertDialog(
            onDismissRequest = { stage = LogoutStage.IDLE },
            title = { Text("Couldn't back up to the cloud") },
            text = {
                Text(
                    "If you log out now, anything not yet synced will be permanently deleted from this device. " +
                        "Check your connection and try again, or log out anyway."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        stage = LogoutStage.IDLE
                        onLogout()
                    },
                    modifier = Modifier.testTag("force_logout_button")
                ) { Text("Log out anyway", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { stage = LogoutStage.IDLE }) { Text("Cancel") } }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("profile_shortcuts_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TechSurfaceDeepNavy),
        border = BorderStroke(1.dp, TechBorderSubtle)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            ShortcutRow(
                icon = Icons.Default.Analytics,
                title = "Sales Reports",
                subtitle = "Revenue, profit and trends",
                onClick = onOpenReports,
                tag = "shortcut_reports"
            )
            HorizontalDivider(color = TechBorderSubtle, modifier = Modifier.padding(horizontal = 18.dp))
            ShortcutRow(
                icon = Icons.Default.FileUpload,
                title = "Data Import",
                subtitle = "Excel migration and CSV tools",
                onClick = onOpenImport,
                tag = "shortcut_import"
            )
            HorizontalDivider(color = TechBorderSubtle, modifier = Modifier.padding(horizontal = 18.dp))
            ShortcutRow(
                icon = Icons.Default.People,
                title = "Customers",
                subtitle = "Customer list and contact details",
                onClick = onOpenCustomers,
                tag = "shortcut_customers"
            )
            HorizontalDivider(color = TechBorderSubtle, modifier = Modifier.padding(horizontal = 18.dp))
            ShortcutRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Log out",
                subtitle = "Sign out of this device",
                onClick = { stage = LogoutStage.CONFIRM },
                tag = "shortcut_logout",
                danger = true
            )
        }
    }
}

@Composable
private fun ShortcutRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String,
    danger: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = if (danger) MaterialTheme.colorScheme.error else TechAccentCyan, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (danger) MaterialTheme.colorScheme.error else TechTextPrimary)
                Text(subtitle, fontSize = 12.sp, color = TechTextSilver)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TechTextSilver)
    }
}
