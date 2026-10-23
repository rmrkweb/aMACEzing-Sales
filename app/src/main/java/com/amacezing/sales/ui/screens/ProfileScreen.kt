package com.amacezing.sales.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.amacezing.sales.model.BusinessProfile
import com.amacezing.sales.ui.theme.SecondaryAmber
import com.amacezing.sales.ui.theme.StatusPaid
import com.amacezing.sales.ui.theme.TechAccentCyan
import com.amacezing.sales.ui.theme.TechAccentViolet
import com.amacezing.sales.ui.theme.TechBorderSubtle
import com.amacezing.sales.ui.theme.TechSurfaceDeepNavy
import com.amacezing.sales.ui.theme.TechSurfaceSecondary
import com.amacezing.sales.ui.theme.TechTextPrimary
import com.amacezing.sales.ui.theme.TechTextSecondary
import com.amacezing.sales.ui.theme.TechTextSilver
import com.amacezing.sales.viewmodel.SalesViewModel
import com.amacezing.sales.viewmodel.SyncStatus
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun ProfileScreen(
    viewModel: SalesViewModel,
    currentUser: FirebaseUser?,
    modifier: Modifier = Modifier,
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val savedProfile by viewModel.businessProfile.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    // Editable State initialized from current active profile
    var businessName by remember(savedProfile) { mutableStateOf(savedProfile.businessName) }
    var tagline by remember(savedProfile) { mutableStateOf(savedProfile.tagline) }
    var ownerName by remember(savedProfile) { mutableStateOf(savedProfile.ownerName.ifBlank { currentUser?.displayName ?: "" }) }
    var contactNumber by remember(savedProfile) { mutableStateOf(savedProfile.contactNumber) }
    var contactEmail by remember(savedProfile) { mutableStateOf(savedProfile.contactEmail.ifBlank { currentUser?.email ?: "" }) }
    var address by remember(savedProfile) { mutableStateOf(savedProfile.address) }
    var facebookPage by remember(savedProfile) { mutableStateOf(savedProfile.facebookPage) }
    var website by remember(savedProfile) { mutableStateOf(savedProfile.website) }
    var tin by remember(savedProfile) { mutableStateOf(savedProfile.tin) }
    var documentFooter by remember(savedProfile) { mutableStateOf(savedProfile.documentFooter) }
    var logoUri by remember(savedProfile) { mutableStateOf(savedProfile.logoUri) }

    // Business Preferences
    var currency by remember(savedProfile) { mutableStateOf(savedProfile.currency) }
    var validityDaysText by remember(savedProfile) { mutableStateOf(savedProfile.quotationValidityDays.toString()) }
    var paymentInstructions by remember(savedProfile) { mutableStateOf(savedProfile.paymentInstructions) }
    var quotationTerms by remember(savedProfile) { mutableStateOf(savedProfile.quotationTerms) }
    var receiptFooter by remember(savedProfile) { mutableStateOf(savedProfile.receiptFooter) }
    var showLogoOnDocuments by remember(savedProfile) { mutableStateOf(savedProfile.showLogoOnDocuments) }
    var showContactOnDocuments by remember(savedProfile) { mutableStateOf(savedProfile.showContactOnDocuments) }

    var isSaving by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Validation state
    val isBusinessNameValid = businessName.trim().isNotBlank()
    val validityDaysInt = validityDaysText.toIntOrNull()
    val isValidityDaysValid = validityDaysInt != null && validityDaysInt > 0
    val isFormValid = isBusinessNameValid && isValidityDaysValid

    // Track unsaved modifications (dirty state)
    val hasUnsavedChanges = remember(
        savedProfile,
        businessName, tagline, ownerName, contactNumber, contactEmail, address,
        facebookPage, website, tin, documentFooter, logoUri, currency,
        validityDaysText, paymentInstructions, quotationTerms, receiptFooter,
        showLogoOnDocuments, showContactOnDocuments
    ) {
        businessName.trim() != savedProfile.businessName ||
                tagline.trim() != savedProfile.tagline ||
                ownerName.trim() != savedProfile.ownerName ||
                contactNumber.trim() != savedProfile.contactNumber ||
                contactEmail.trim() != savedProfile.contactEmail ||
                address.trim() != savedProfile.address ||
                facebookPage.trim() != savedProfile.facebookPage ||
                website.trim() != savedProfile.website ||
                tin.trim() != savedProfile.tin ||
                documentFooter.trim() != savedProfile.documentFooter ||
                logoUri != savedProfile.logoUri ||
                currency.trim() != savedProfile.currency ||
                validityDaysInt != savedProfile.quotationValidityDays ||
                paymentInstructions.trim() != savedProfile.paymentInstructions ||
                quotationTerms.trim() != savedProfile.quotationTerms ||
                receiptFooter.trim() != savedProfile.receiptFooter ||
                showLogoOnDocuments != savedProfile.showLogoOnDocuments ||
                showContactOnDocuments != savedProfile.showContactOnDocuments
    }

    // Android Photo Picker for Business Logo selection
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { pickedUri: Uri? ->
        if (pickedUri != null) {
            coroutineScope.launch {
                // Copy to local internal files directory so the URI remains persistent across app restarts!
                withContext(Dispatchers.IO) {
                    try {
                        val brandingDir = File(context.filesDir, "branding")
                        if (!brandingDir.exists()) brandingDir.mkdirs()
                        val destinationFile = File(brandingDir, "business_logo.png")

                        context.contentResolver.openInputStream(pickedUri)?.use { input ->
                            FileOutputStream(destinationFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        logoUri = destinationFile.absolutePath
                    } catch (_: Exception) {
                        logoUri = pickedUri.toString()
                    }
                }
            }
        }
    }

    // Unsaved Changes Back Handler
    BackHandler(enabled = hasUnsavedChanges) {
        showDiscardDialog = true
    }

    fun resetToSaved() {
        businessName = savedProfile.businessName
        tagline = savedProfile.tagline
        ownerName = savedProfile.ownerName
        contactNumber = savedProfile.contactNumber
        contactEmail = savedProfile.contactEmail
        address = savedProfile.address
        facebookPage = savedProfile.facebookPage
        website = savedProfile.website
        tin = savedProfile.tin
        documentFooter = savedProfile.documentFooter
        logoUri = savedProfile.logoUri
        currency = savedProfile.currency
        validityDaysText = savedProfile.quotationValidityDays.toString()
        paymentInstructions = savedProfile.paymentInstructions
        quotationTerms = savedProfile.quotationTerms
        receiptFooter = savedProfile.receiptFooter
        showLogoOnDocuments = savedProfile.showLogoOnDocuments
        showContactOnDocuments = savedProfile.showContactOnDocuments
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (hasUnsavedChanges) {
                                showDiscardDialog = true
                            } else {
                                viewModel.handleBack()
                            }
                        },
                        modifier = Modifier.testTag("profile_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TechTextSilver
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Profile & Branding",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = TechAccentCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = TechAccentViolet.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, TechAccentViolet.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "CENTRAL SOURCE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TechAccentViolet,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Manage business identity, documents branding, and contact details",
                            style = MaterialTheme.typography.bodySmall,
                            color = TechTextSilver
                        )
                    }
                }
            }

            // Save / Discard Status Bar (Visible when there are unsaved changes)
            if (hasUnsavedChanges) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("unsaved_changes_banner"),
                        shape = RoundedCornerShape(12.dp),
                        color = TechAccentViolet.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TechAccentViolet.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = TechAccentViolet,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "You have unsaved changes",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TechTextPrimary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = { resetToSaved() },
                                    modifier = Modifier.testTag("discard_changes_button")
                                ) {
                                    Text("Discard", fontSize = 12.sp, color = TechTextSilver)
                                }

                                Button(
                                    onClick = {
                                        if (isFormValid) {
                                            isSaving = true
                                            val updated = savedProfile.copy(
                                                userId = currentUser?.uid ?: savedProfile.userId,
                                                businessName = businessName.trim(),
                                                tagline = tagline.trim(),
                                                ownerName = ownerName.trim(),
                                                contactNumber = contactNumber.trim(),
                                                contactEmail = contactEmail.trim(),
                                                address = address.trim(),
                                                facebookPage = facebookPage.trim(),
                                                website = website.trim(),
                                                tin = tin.trim(),
                                                documentFooter = documentFooter.trim(),
                                                logoUri = logoUri,
                                                currency = currency.trim().ifEmpty { "PHP" },
                                                quotationValidityDays = validityDaysInt ?: 7,
                                                paymentInstructions = paymentInstructions.trim(),
                                                quotationTerms = quotationTerms.trim(),
                                                receiptFooter = receiptFooter.trim(),
                                                showLogoOnDocuments = showLogoOnDocuments,
                                                showContactOnDocuments = showContactOnDocuments,
                                                updatedAtEpochMillis = System.currentTimeMillis()
                                            )
                                            viewModel.updateBusinessProfile(updated) {
                                                isSaving = false
                                            }
                                        }
                                    },
                                    enabled = isFormValid && !isSaving,
                                    colors = ButtonDefaults.buttonColors(containerColor = TechAccentCyan),
                                    modifier = Modifier.testTag("save_profile_button")
                                ) {
                                    if (isSaving) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.Black
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Save,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 1. Authenticated User Profile Card (Read-only Identity)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_profile_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TechSurfaceDeepNavy),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TechBorderSubtle)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Authenticated Identity",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TechAccentCyan
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (syncStatus) {
                                    SyncStatus.SYNCED -> StatusPaid.copy(alpha = 0.15f)
                                    SyncStatus.SYNCING -> TechAccentCyan.copy(alpha = 0.15f)
                                    SyncStatus.OFFLINE_SAVED -> SecondaryAmber.copy(alpha = 0.15f)
                                    SyncStatus.SYNC_ERROR -> MaterialTheme.colorScheme.errorContainer
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    when (syncStatus) {
                                        SyncStatus.SYNCED -> StatusPaid.copy(alpha = 0.4f)
                                        SyncStatus.SYNCING -> TechAccentCyan.copy(alpha = 0.4f)
                                        SyncStatus.OFFLINE_SAVED -> SecondaryAmber.copy(alpha = 0.4f)
                                        SyncStatus.SYNC_ERROR -> MaterialTheme.colorScheme.error
                                    }
                                )
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
                                        modifier = Modifier.size(13.dp),
                                        tint = when (syncStatus) {
                                            SyncStatus.SYNCED -> StatusPaid
                                            SyncStatus.SYNCING -> TechAccentCyan
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
                                            SyncStatus.SYNCING -> TechAccentCyan
                                            SyncStatus.OFFLINE_SAVED -> SecondaryAmber
                                            SyncStatus.SYNC_ERROR -> MaterialTheme.colorScheme.error
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val photoUrl = currentUser?.photoUrl?.toString()
                            if (!photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "User Profile Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, TechAccentCyan, CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(TechAccentCyan.copy(alpha = 0.2f))
                                        .border(2.dp, TechAccentCyan.copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initial = currentUser?.displayName?.firstOrNull()?.uppercaseChar()
                                        ?: currentUser?.email?.firstOrNull()?.uppercaseChar()
                                        ?: 'A'
                                    Text(
                                        text = initial.toString(),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TechAccentCyan
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = currentUser?.displayName?.ifBlank { "aMACEzing Operator" } ?: "aMACEzing Operator",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TechTextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentUser?.email ?: "Offline Account",
                                    fontSize = 13.sp,
                                    color = TechTextSilver
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        tint = TechAccentViolet,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Google Sign-In Authenticated",
                                        fontSize = 11.sp,
                                        color = TechAccentViolet,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = TechBorderSubtle)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Authentication identity is securely verified by Google and cannot be modified locally. Editable business contact and document branding fields are configured below.",
                            fontSize = 11.sp,
                            color = TechTextSilver,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Shortcuts to Reports / Data Import / Customers
            item {
                com.amacezing.sales.ui.components.ProfileShortcutsCard(
                    onOpenReports = { viewModel.navigateTo(com.amacezing.sales.ui.navigation.Screen.REPORTS) },
                    onOpenImport = { viewModel.navigateTo(com.amacezing.sales.ui.navigation.Screen.IMPORT_EXPORT) },
                    onOpenCustomers = { viewModel.navigateTo(com.amacezing.sales.ui.navigation.Screen.CUSTOMERS) },
                    syncBeforeLogout = { viewModel.syncBeforeLogout() },
                    syncErrorDetail = { viewModel.lastSyncError.value },
                    onLogout = onSignOut
                )
            }

            

            // App lock (biometric) toggle
            item {
                com.amacezing.sales.ui.components.BiometricLockCard()
            }

            // 2. Business Branding Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("business_branding_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TechSurfaceDeepNavy),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TechBorderSubtle)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = null,
                                tint = TechAccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Business Branding & Logo",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TechTextPrimary
                            )
                        }
                        Text(
                            text = "Branding rendered on finalized quotations, receipts, and invoices",
                            fontSize = 12.sp,
                            color = TechTextSilver
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Logo Section
                        Text("Business Document Logo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TechTextPrimary)
                        Text(
                            "Separate from Android launcher icon. Rendered on header of invoices and estimates.",
                            fontSize = 11.sp,
                            color = TechTextSilver
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(TechSurfaceSecondary, RoundedCornerShape(12.dp))
                                .border(1.dp, TechBorderSubtle, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            if (logoUri.isNotBlank()) {
                                AsyncImage(
                                    model = if (logoUri.startsWith("content://") || logoUri.startsWith("file://") || logoUri.startsWith("http")) {
                                        logoUri
                                    } else {
                                        File(logoUri)
                                    },
                                    contentDescription = "Business Logo Preview",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .padding(4.dp)
                                        .border(1.dp, TechBorderSubtle, RoundedCornerShape(8.dp))
                                        .testTag("logo_preview_image")
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(TechSurfaceDeepNavy)
                                        .border(1.dp, TechBorderSubtle, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Business,
                                        contentDescription = "No Logo",
                                        tint = TechTextSilver,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TechAccentCyan),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("select_logo_button")
                                ) {
                                    Icon(
                                        imageVector = if (logoUri.isNotBlank()) Icons.Default.Edit else Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (logoUri.isNotBlank()) "Change Logo" else "Upload Logo",
                                        fontSize = 12.sp,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (logoUri.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedButton(
                                        onClick = { logoUri = "" },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.testTag("remove_logo_button")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Remove", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Business Name (Required)
                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            label = { Text("Business Name *") },
                            isError = !isBusinessNameValid,
                            supportingText = {
                                if (!isBusinessNameValid) {
                                    Text("Business name is required for document generation", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TechAccentCyan,
                                unfocusedBorderColor = TechBorderSubtle,
                                focusedTextColor = TechTextPrimary,
                                unfocusedTextColor = TechTextPrimary,
                                focusedLabelColor = TechAccentCyan,
                                unfocusedLabelColor = TechTextSilver
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("business_name_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tagline
                        OutlinedTextField(
                            value = tagline,
                            onValueChange = { tagline = it },
                            label = { Text("Tagline / Description") },
                            placeholder = { Text("e.g. Precision Laser Engraving & Sintra Board Printing") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TechAccentCyan,
                                unfocusedBorderColor = TechBorderSubtle,
                                focusedTextColor = TechTextPrimary,
                                unfocusedTextColor = TechTextPrimary,
                                focusedLabelColor = TechAccentCyan,
                                unfocusedLabelColor = TechTextSilver
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("business_tagline_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Owner / Authorized Representative
                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = { Text("Owner / Authorized Representative") },
                            placeholder = { Text("e.g. Reymark Aguila") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TechAccentCyan,
                                unfocusedBorderColor = TechBorderSubtle,
                                focusedTextColor = TechTextPrimary,
                                unfocusedTextColor = TechTextPrimary,
                                focusedLabelColor = TechAccentCyan,
                                unfocusedLabelColor = TechTextSilver
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("business_owner_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Contact Number & Email in Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = contactNumber,
                                onValueChange = { contactNumber = it },
                                label = { Text("Contact Number") },
                                placeholder = { Text("0912 345 6789") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TechAccentCyan,
                                    unfocusedBorderColor = TechBorderSubtle,
                                    focusedTextColor = TechTextPrimary,
                                    unfocusedTextColor = TechTextPrimary,
                                    focusedLabelColor = TechAccentCyan,
                                    unfocusedLabelColor = TechTextSilver
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("business_phone_input")
                            )

                            OutlinedTextField(
                                value = contactEmail,
                                onValueChange = { contactEmail = it },
                                label = { Text("Business Email") },
                                placeholder = { Text("contact@amacezing.com") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TechAccentCyan,
                                    unfocusedBorderColor = TechBorderSubtle,
                                    focusedTextColor = TechTextPrimary,
                                    unfocusedTextColor = TechTextPrimary,
                                    focusedLabelColor = TechAccentCyan,
                                    unfocusedLabelColor = TechTextSilver
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("business_email_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Business Address
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Business Physical Address") },
                            placeholder = { Text("e.g. San Jose del Monte, Bulacan, Philippines") },
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TechAccentCyan,
                                unfocusedBorderColor = TechBorderSubtle,
                                focusedTextColor = TechTextPrimary,
                                unfocusedTextColor = TechTextPrimary,
                                focusedLabelColor = TechAccentCyan,
                                unfocusedLabelColor = TechTextSilver
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("business_address_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Facebook Page & Website
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = facebookPage,
                                onValueChange = { facebookPage = it },
                                label = { Text("Facebook Page") },
                                placeholder = { Text("facebook.com/aMACEzing") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TechAccentCyan,
                                    unfocusedBorderColor = TechBorderSubtle,
                                    focusedTextColor = TechTextPrimary,
                                    unfocusedTextColor = TechTextPrimary,
                                    focusedLabelColor = TechAccentCyan,
                                    unfocusedLabelColor = TechTextSilver
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("business_facebook_input")
                            )

                            OutlinedTextField(
                                value = website,
                                onValueChange = { website = it },
                                label = { Text("Website") },
                                placeholder = { Text("https://amacezing.com") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TechAccentCyan,
                                    unfocusedBorderColor = TechBorderSubtle,
                                    focusedTextColor = TechTextPrimary,
                                    unfocusedTextColor = TechTextPrimary,
                                    focusedLabelColor = TechAccentCyan,
                                    unfocusedLabelColor = TechTextSilver
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("business_website_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // TIN (Optional)
                        OutlinedTextField(
                            value = tin,
                            onValueChange = { tin = it },
                            label = { Text("TIN (Taxpayer Identification Number - Optional)") },
                            placeholder = { Text("000-000-000-000") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TechAccentCyan,
                                unfocusedBorderColor = TechBorderSubtle,
                                focusedTextColor = TechTextPrimary,
                                unfocusedTextColor = TechTextPrimary,
                                focusedLabelColor = TechAccentCyan,
                                unfocusedLabelColor = TechTextSilver
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("business_tin_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Default Document Footer
                        OutlinedTextField(
                            value = documentFooter,
                            onValueChange = { documentFooter = it },
                            label = { Text("Default Document Footer") },
                            placeholder = { Text("e.g. Thank you for your business! All orders subject to quality guarantee.") },
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TechAccentCyan,
                                unfocusedBorderColor = TechBorderSubtle,
                                focusedTextColor = TechTextPrimary,
                                unfocusedTextColor = TechTextPrimary,
                                focusedLabelColor = TechAccentCyan,
                                unfocusedLabelColor = TechTextSilver
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("business_footer_input")
                        )
                    }
                }
            }

            // 3. Business Preferences Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("business_preferences_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TechSurfaceDeepNavy),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TechBorderSubtle)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = TechAccentViolet,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Document & Quotation Preferences",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TechTextPrimary
                            )
                        }
                        Text(
                            text = "Default terms, validity periods, and document layout controls",
                            fontSize = 12.sp,
                            color = TechTextSilver
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Currency and Validity Days
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = currency,
                                onValueChange = { currency = it },
                                label = { Text("Currency Code") },
                                placeholder = { Text("PHP") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TechAccentViolet,
                                    unfocusedBorderColor = TechBorderSubtle,
                                    focusedTextColor = TechTextPrimary,
                                    unfocusedTextColor = TechTextPrimary,
                                    focusedLabelColor = TechAccentViolet,
                                    unfocusedLabelColor = TechTextSilver
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("currency_input")
                            )

                            OutlinedTextField(
                                value = validityDaysText,
                                onValueChange = { validityDaysText = it },
                                label = { Text("Validity Period (Days) *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = !isValidityDaysValid,
                                supportingText = {
                                    if (!isValidityDaysValid) {
                                        Text("Must be at least 1 day", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TechAccentViolet,
                                    unfocusedBorderColor = TechBorderSubtle,
                                    focusedTextColor = TechTextPrimary,
                                    unfocusedTextColor = TechTextPrimary,
                                    focusedLabelColor = TechAccentViolet,
                                    unfocusedLabelColor = TechTextSilver
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("validity_days_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Payment Instructions
                        OutlinedTextField(
                            value = paymentInstructions,
                            onValueChange = { paymentInstructions = it },
                            label = { Text("Default Payment Instructions") },
                            placeholder = { Text("e.g. GCash: 0912-345-6789 / BDO: 1234-5678-90") },
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TechAccentViolet,
                                unfocusedBorderColor = TechBorderSubtle,
                                focusedTextColor = TechTextPrimary,
                                unfocusedTextColor = TechTextPrimary,
                                focusedLabelColor = TechAccentViolet,
                                unfocusedLabelColor = TechTextSilver
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_instructions_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quotation Terms & Conditions
                        OutlinedTextField(
                            value = quotationTerms,
                            onValueChange = { quotationTerms = it },
                            label = { Text("Default Quotation Terms & Conditions") },
                            placeholder = { Text("e.g. 50% downpayment required before production...") },
                            minLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TechAccentViolet,
                                unfocusedBorderColor = TechBorderSubtle,
                                focusedTextColor = TechTextPrimary,
                                unfocusedTextColor = TechTextPrimary,
                                focusedLabelColor = TechAccentViolet,
                                unfocusedLabelColor = TechTextSilver
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quotation_terms_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Receipt Footer
                        OutlinedTextField(
                            value = receiptFooter,
                            onValueChange = { receiptFooter = it },
                            label = { Text("Receipt Footer") },
                            placeholder = { Text("e.g. This serves as official sales proof of payment.") },
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TechAccentViolet,
                                unfocusedBorderColor = TechBorderSubtle,
                                focusedTextColor = TechTextPrimary,
                                unfocusedTextColor = TechTextPrimary,
                                focusedLabelColor = TechAccentViolet,
                                unfocusedLabelColor = TechTextSilver
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("receipt_footer_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = TechBorderSubtle)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Document Display Toggles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Show Logo on Generated Documents", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TechTextPrimary)
                                Text("When enabled, business logo is rendered on header", fontSize = 11.sp, color = TechTextSilver)
                            }
                            Switch(
                                checked = showLogoOnDocuments,
                                onCheckedChange = { showLogoOnDocuments = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = TechAccentCyan, checkedTrackColor = TechAccentCyan.copy(alpha = 0.5f)),
                                modifier = Modifier.testTag("toggle_show_logo")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Show Contact Details on Documents", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TechTextPrimary)
                                Text("Include phone, email, address, and social handles", fontSize = 11.sp, color = TechTextSilver)
                            }
                            Switch(
                                checked = showContactOnDocuments,
                                onCheckedChange = { showContactOnDocuments = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = TechAccentViolet, checkedTrackColor = TechAccentViolet.copy(alpha = 0.5f)),
                                modifier = Modifier.testTag("toggle_show_contact")
                            )
                        }
                    }
                }
            }

            // Primary Bottom Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (hasUnsavedChanges) {
                                showDiscardDialog = true
                            } else {
                                viewModel.handleBack()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("bottom_cancel_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TechTextSilver)
                    ) {
                        Text("Cancel / Back", fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            if (isFormValid) {
                                isSaving = true
                                val updated = savedProfile.copy(
                                    userId = currentUser?.uid ?: savedProfile.userId,
                                    businessName = businessName.trim(),
                                    tagline = tagline.trim(),
                                    ownerName = ownerName.trim(),
                                    contactNumber = contactNumber.trim(),
                                    contactEmail = contactEmail.trim(),
                                    address = address.trim(),
                                    facebookPage = facebookPage.trim(),
                                    website = website.trim(),
                                    tin = tin.trim(),
                                    documentFooter = documentFooter.trim(),
                                    logoUri = logoUri,
                                    currency = currency.trim().ifEmpty { "PHP" },
                                    quotationValidityDays = validityDaysInt ?: 7,
                                    paymentInstructions = paymentInstructions.trim(),
                                    quotationTerms = quotationTerms.trim(),
                                    receiptFooter = receiptFooter.trim(),
                                    showLogoOnDocuments = showLogoOnDocuments,
                                    showContactOnDocuments = showContactOnDocuments,
                                    updatedAtEpochMillis = System.currentTimeMillis()
                                )
                                viewModel.updateBusinessProfile(updated) {
                                    isSaving = false
                                }
                            }
                        },
                        enabled = isFormValid && !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = TechAccentCyan),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("bottom_save_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...", color = Color.Black, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Profile", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }

    // Discard Changes Confirmation Dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = {
                Text(
                    text = "Discard Unsaved Changes?",
                    fontWeight = FontWeight.Bold,
                    color = TechTextPrimary
                )
            },
            text = {
                Text(
                    text = "You have unsaved edits in your Business Profile. If you discard, all changes will be reverted.",
                    color = TechTextSilver
                )
            },
            confirmButton = {
                com.amacezing.sales.ui.components.DestructiveActionButton(
                    label = "Discard Changes",
                    onClick = {
                        resetToSaved()
                        showDiscardDialog = false
                        viewModel.handleBack()
                    },
                    modifier = Modifier.testTag("confirm_discard_button")
                )
            },
            dismissButton = {
                com.amacezing.sales.ui.components.DismissActionButton(
                    label = "Keep Editing",
                    icon = Icons.Default.Edit,
                    onClick = { showDiscardDialog = false },
                    modifier = Modifier.testTag("cancel_discard_button")
                )
            },
            containerColor = TechSurfaceDeepNavy,
            modifier = Modifier.testTag("discard_dialog")
        )
    }
}
