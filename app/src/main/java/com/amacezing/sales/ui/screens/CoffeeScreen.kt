package com.amacezing.sales.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.amacezing.sales.ui.components.PrimaryActionButton
import com.amacezing.sales.ui.components.SecondaryActionButton
import com.amacezing.sales.ui.theme.AppBorder
import com.amacezing.sales.util.CoffeeTip
import com.amacezing.sales.viewmodel.SalesViewModel
import kotlinx.coroutines.launch

/**
 * "Buy me a coffee" page: shows the tip QR (loaded from GitHub so it can change without an app update),
 * lets the user save it to the gallery, and explains how to send a tip.
 */
@Composable
fun CoffeeScreen(
    viewModel: SalesViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }

    val request = remember(retry) {
        ImageRequest.Builder(context)
            .data(CoffeeTip.imageUrl)
            // Cache for at most 5 minutes: the key changes with each 5-minute window.
            .memoryCacheKey(CoffeeTip.cacheKey())
            .diskCacheKey(CoffeeTip.cacheKey())
            .setParameter("retry", retry)
            .crossfade(true)
            .build()
    }

    fun openInBrowser() {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CoffeeTip.imageUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { viewModel.snackbarMessage.value = "Couldn't open the QR image." }
    }

    fun viewSaved(uri: Uri) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, context.contentResolver.getType(uri) ?: "image/*")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("coffee_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Buy me a coffee", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Support the open source app ♡",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, AppBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("coffee_qr_card")
        ) {
            SubcomposeAsyncImage(
                model = request,
                contentDescription = "QR code for sending a coffee tip to the developer",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("coffee_qr_image"),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    }
                },
                error = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .testTag("coffee_qr_error"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Couldn't load the QR code. Check your connection and try again.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SecondaryActionButton(
                            label = "Try again",
                            icon = Icons.Default.Refresh,
                            onClick = { retry++ },
                            modifier = Modifier.testTag("coffee_qr_retry_button")
                        )
                    }
                }
            )
        }

        PrimaryActionButton(
            label = if (saving) "Saving…" else "View / Download QR",
            icon = Icons.Default.Download,
            enabled = !saving,
            onClick = {
                if (!CoffeeTip.canSaveToGallery) {
                    // Older Android versions can't save to the gallery without a storage permission.
                    openInBrowser()
                } else {
                    scope.launch {
                        saving = true
                        val uri = CoffeeTip.saveToGallery(context)
                        saving = false
                        if (uri == null) {
                            viewModel.snackbarMessage.value = "Couldn't save the QR. Check your connection and try again."
                        } else {
                            viewModel.snackbarMessage.value = "QR saved to Pictures/aMACEzing."
                            viewSaved(uri)
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("coffee_download_qr_button")
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, AppBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("coffee_instructions_card")
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("How to send a tip", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                listOf(
                    "1. Tap View / Download QR to save the code to your phone.",
                    "2. Open your bank or e-wallet app, choose Scan QR, then pick the saved image from your gallery.",
                    "3. Enter any amount you like and add a thankful message."
                ).forEach {
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "Thank you for supporting this open source app ♡",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
