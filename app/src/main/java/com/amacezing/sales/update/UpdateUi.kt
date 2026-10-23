package com.amacezing.sales.update

import androidx.compose.material.icons.filled.Check
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amacezing.sales.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.testTag

sealed interface UpdatePhase {
    data object Idle : UpdatePhase
    data object Checking : UpdatePhase
    data object UpToDate : UpdatePhase
    data class Available(val info: UpdateInfo) : UpdatePhase
    data class Downloading(val progress: Float) : UpdatePhase
    data class NeedsPermission(val apk: File) : UpdatePhase
    data class Error(val message: String) : UpdatePhase
}

class UpdateController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    var phase by mutableStateOf<UpdatePhase>(UpdatePhase.Idle)
        private set
    private var info: UpdateInfo? = null

    /** silent = background check: only surface something if an update exists. */
    fun check(silent: Boolean) {
        if (phase is UpdatePhase.Checking || phase is UpdatePhase.Downloading) return
        phase = UpdatePhase.Checking
        scope.launch {
            phase = when (val r = AppUpdater.check()) {
                is CheckResult.Available -> { info = r.info; UpdatePhase.Available(r.info) }
                CheckResult.UpToDate -> if (silent) UpdatePhase.Idle else UpdatePhase.UpToDate
                CheckResult.Disabled ->
                    if (silent) UpdatePhase.Idle else UpdatePhase.Error("Updates are not enabled in this build.")
                is CheckResult.Error -> if (silent) UpdatePhase.Idle else UpdatePhase.Error(r.message)
            }
        }
    }

    fun download() {
        val target = info ?: return
        phase = UpdatePhase.Downloading(0f)
        scope.launch {
            try {
                val apk = AppUpdater.download(context, target) { phase = UpdatePhase.Downloading(it) }
                install(apk)
            } catch (e: Exception) {
                phase = UpdatePhase.Error(e.message ?: "Download failed")
            }
        }
    }

    fun install(apk: File) {
        phase = if (AppUpdater.install(context, apk)) UpdatePhase.Idle else UpdatePhase.NeedsPermission(apk)
    }

    fun dismiss() { phase = UpdatePhase.Idle }
}

@Composable
fun rememberUpdateController(): UpdateController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember { UpdateController(context, scope) }
}

private var autoCheckedThisSession = false

/** Place once after login: silently checks on launch and shows a dialog if an update exists. */
@Composable
fun AutoUpdatePrompt() {
    val controller = rememberUpdateController()
    LaunchedEffect(Unit) {
        if (!autoCheckedThisSession) {
            autoCheckedThisSession = true
            controller.check(silent = true)
        }
    }
    UpdateDialog(controller)
}

/** Settings card with current version information and a manual "Check for updates" option. */
@Composable
fun UpdateSettingsCard(modifier: Modifier = Modifier) {
    val controller = rememberUpdateController()
    Card(
        modifier = modifier.fillMaxWidth().testTag("update_settings_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "App Version & Updates",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Build code ${BuildConfig.VERSION_CODE} • Channel: GitHub Release",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (BuildConfig.GITHUB_REPO.isNotBlank()) {
                Text(
                    text = "Source: github.com/${BuildConfig.GITHUB_REPO}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (controller.phase) {
                        is UpdatePhase.Checking -> "Checking for updates..."
                        is UpdatePhase.Available -> "Update available!"
                        is UpdatePhase.UpToDate -> "App is up to date."
                        is UpdatePhase.Downloading -> "Downloading update..."
                        is UpdatePhase.Error -> "Check failed. Try again."
                        else -> "Check for latest release on GitHub"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = { controller.check(silent = false) },
                    enabled = controller.phase !is UpdatePhase.Checking && controller.phase !is UpdatePhase.Downloading,
                    modifier = Modifier.testTag("check_for_updates_button")
                ) {
                    if (controller.phase is UpdatePhase.Checking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checking...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check for Updates")
                    }
                }
            }
        }
    }
    UpdateDialog(controller)
}

@Composable
private fun UpdateDialog(controller: UpdateController) {
    when (val p = controller.phase) {
        UpdatePhase.Idle, UpdatePhase.Checking -> Unit

        UpdatePhase.UpToDate -> AlertDialog(
            onDismissRequest = controller::dismiss,
            title = { Text("You're up to date") },
            text = { Text("v${BuildConfig.VERSION_NAME} is the latest version.") },
            confirmButton = { com.amacezing.sales.ui.components.PrimaryActionButton(label = "OK", icon = Icons.Default.Check, onClick = controller::dismiss) }
        )

        is UpdatePhase.Available -> AlertDialog(
            onDismissRequest = controller::dismiss,
            title = { Text("Update available") },
            text = {
                Column {
                    Text("Version ${p.info.versionName} (build ${p.info.versionCode})", fontWeight = FontWeight.SemiBold)
                    if (p.info.sizeBytes > 0) {
                        Text("%.1f MB".format(p.info.sizeBytes / 1_048_576.0), fontSize = 12.sp)
                    }
                    if (p.info.notes.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(p.info.notes, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = { com.amacezing.sales.ui.components.PrimaryActionButton(label = "Download & install", icon = Icons.Default.CloudDownload, onClick = controller::download) },
            dismissButton = { com.amacezing.sales.ui.components.DismissActionButton(label = "Later", onClick = controller::dismiss) }
        )

        is UpdatePhase.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Downloading update…") },
            text = {
                Column {
                    LinearProgressIndicator(progress = { p.progress }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("${(p.progress * 100).toInt()}%", fontSize = 12.sp)
                }
            },
            confirmButton = {}
        )

        is UpdatePhase.NeedsPermission -> AlertDialog(
            onDismissRequest = controller::dismiss,
            title = { Text("Allow installs") },
            text = {
                Text("Turn on “Allow from this source” for aMACEzing Sales in the settings screen that opened, come back, then tap Install.")
            },
            confirmButton = { com.amacezing.sales.ui.components.PrimaryActionButton(label = "Install", icon = Icons.Default.CloudDownload, onClick = { controller.install(p.apk) }) },
            dismissButton = { com.amacezing.sales.ui.components.DismissActionButton(label = "Cancel", onClick = controller::dismiss) }
        )

        is UpdatePhase.Error -> AlertDialog(
            onDismissRequest = controller::dismiss,
            title = { Text("Update problem") },
            text = { Text(p.message) },
            confirmButton = { com.amacezing.sales.ui.components.PrimaryActionButton(label = "OK", icon = Icons.Default.Check, onClick = controller::dismiss) }
        )
    }
}
