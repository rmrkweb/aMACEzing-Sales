package com.amacezing.sales.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amacezing.sales.security.BiometricAuth
import com.amacezing.sales.security.SecurityPrefs
import com.amacezing.sales.security.findFragmentActivity
import com.amacezing.sales.ui.theme.TechAccentCyan
import com.amacezing.sales.ui.theme.TechBorderSubtle
import com.amacezing.sales.ui.theme.TechSurfaceDeepNavy
import com.amacezing.sales.ui.theme.TechTextPrimary
import com.amacezing.sales.ui.theme.TechTextSilver

/** Profile toggle: require fingerprint / face / device PIN when opening the app. Applies instantly. */
@Composable
fun BiometricLockCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(SecurityPrefs.isBiometricLockEnabled(context)) }
    var message by remember { mutableStateOf<String?>(null) }

    fun change(target: Boolean) {
        message = null
        val activity = context.findFragmentActivity()
        if (activity == null) {
            message = "Biometric prompt is not available here."
            return
        }
        if (!BiometricAuth.canAuthenticate(context)) {
            message = "Set up a fingerprint, face unlock or screen lock in your device settings first."
            return
        }
        // Confirm identity before turning the lock on or off.
        BiometricAuth.authenticate(
            activity = activity,
            title = if (target) "Turn on app lock" else "Turn off app lock",
            subtitle = "Confirm it's you",
            onSuccess = {
                SecurityPrefs.setBiometricLockEnabled(context, target)
                enabled = target
            },
            onFailure = { message = it }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("biometric_lock_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TechSurfaceDeepNavy),
        border = BorderStroke(1.dp, TechBorderSubtle)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = TechAccentCyan, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("App Lock", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TechTextPrimary)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { change(it) },
                    modifier = Modifier.testTag("biometric_lock_switch")
                )
            }
            Text(
                text = "Require fingerprint, face unlock or your device PIN when opening the app. Applies to this device only.",
                fontSize = 12.sp,
                color = TechTextSilver
            )
            message?.let {
                Text(text = it, fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            }
        }
    }
}
