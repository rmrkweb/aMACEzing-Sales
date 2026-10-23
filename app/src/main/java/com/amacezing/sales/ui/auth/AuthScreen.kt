package com.amacezing.sales.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.amacezing.sales.R
import com.amacezing.sales.ui.theme.TechAccentCyan
import com.amacezing.sales.ui.theme.TechAccentDeepBlue
import com.amacezing.sales.ui.theme.TechBadgeBg
import com.amacezing.sales.ui.theme.TechBgNearBlack
import com.amacezing.sales.ui.theme.TechBorderSubtle
import com.amacezing.sales.ui.theme.TechSurfaceDeepNavy
import com.amacezing.sales.ui.theme.TechSurfaceSecondary
import com.amacezing.sales.ui.theme.TechTextPrimary
import com.amacezing.sales.ui.theme.TechTextSecondary
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val webClientIdRes = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    val clientId = if (webClientIdRes != 0) {
        try {
            context.getString(webClientIdRes).takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    } else {
        null
    }
    if (clientId == null) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val webClientIdRes = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    val clientId = if (webClientIdRes != 0) {
        try {
            context.getString(webClientIdRes).takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    } else {
        null
    }
    if (clientId == null) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found. Please verify your Firebase google-services.json.")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type received during Google Sign-In.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Google Sign-In failed. Please try again.")
        }
    }
}

fun signOut(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

/**
 * Premium Dark Technology Login Screen for aMACEzing Sales.
 * Faithfully recreates the visual design and branding from the high-fidelity UI mockup.
 */
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Attempt silent auto sign-in on launch
    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = { /* Allow interactive sign in */ },
            scope = coroutineScope
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TechBgNearBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        // Decorative geometric tech accents in background
        DecorativeTechAccents()

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            val scrollState = rememberScrollState()

            // Main Card Container with subtle blue edge lighting
            Surface(
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        border = BorderStroke(
                            1.dp,
                            Brush.verticalGradient(
                                colors = listOf(
                                    TechAccentCyan.copy(alpha = 0.6f),
                                    TechBorderSubtle,
                                    TechBorderSubtle.copy(alpha = 0.4f)
                                )
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(28.dp), spotColor = TechAccentCyan.copy(alpha = 0.25f))
                    .testTag("auth_card"),
                color = TechSurfaceDeepNavy,
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    
 // --- 1. Authentic Circular Shop Logo ---
Box(
    modifier = Modifier
        .size(118.dp)
        .clip(CircleShape)
        .background(Color.Transparent),
    contentAlignment = Alignment.Center
) {
    Image(
        painter = painterResource(
            id = R.drawable.ic_launcher_foreground_img
        ),
        contentDescription = "aMACEzing Logo",
        modifier = Modifier.size(112.dp),
        contentScale = ContentScale.Fit
    )
}

Spacer(modifier = Modifier.height(18.dp))


                    // --- 2. Title: "aMACEzing Sales" ---
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "aMACEzing",
                            color = TechAccentCyan,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sales",
                            color = TechTextPrimary,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // --- 3. Subtitle ---
                    Text(
                        text = "Manage Your Business. Grow Your Sales",
                        color = TechTextSecondary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // --- 4. Sleek Cyan Divider Pill ---
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        TechAccentCyan.copy(alpha = 0.3f),
                                        TechAccentCyan,
                                        TechAccentDeepBlue,
                                        TechAccentCyan.copy(alpha = 0.3f)
                                    )
                                )
                            )
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // --- 5. Feature Panel (Dark outlined container with 3 feature rows) ---
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = TechSurfaceSecondary,
                        border = BorderStroke(1.dp, TechBorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Feature 1: Cloud Persistence & Synchronization
                            AuthFeatureItem(
                                icon = Icons.Default.Cloud,
                                title = "Cloud Persistence & Synchronization",
                                description = "Your sales, orders, and receivables stay synchronized across your devices."
                            )

                            // Feature 2: Multiple Businesses in One App
                            AuthFeatureItem(
                                icon = Icons.Default.Storefront,
                                title = "All Your Businesses in One App",
                                description = "Add as many businesses as you need, each tracked independently."
                            )

                            // Feature 3: Secure Business Data
                            AuthFeatureItem(
                                icon = Icons.Default.Security,
                                title = "Zero-Trust Data Privacy",
                                description = "Your transactions are encrypted and isolated to your account."
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- 6. Error Notice (if any) ---
                    if (errorMessage != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF3B0D0D),
                            border = BorderStroke(1.dp, Color(0xFF991B1B))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = Color(0xFFFCA5A5),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = Color(0xFFFEE2E2),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // --- 7. Google Sign-In Button ---
                    GoogleSignInPillButton(
                        isLoading = isLoading,
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { err ->
                                    isLoading = false
                                    errorMessage = err
                                },
                                scope = coroutineScope,
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * Individual Feature Row matching the mockup's circular badge + bold title + description design.
 */
@Composable
private fun AuthFeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular Blue Icon Badge
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            TechAccentDeepBlue.copy(alpha = 0.5f),
                            TechBadgeBg
                        )
                    )
                )
                .border(BorderStroke(1.dp, TechBorderSubtle), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TechAccentCyan,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TechTextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                color = TechTextSecondary,
                fontSize = 11.5.sp,
                lineHeight = 15.sp
            )
        }
    }
}

/**
 * High-contrast Google Sign-in button styled as a rounded pill with official Google G branding.
 */
@Composable
private fun GoogleSignInPillButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .clickable(enabled = !isLoading, onClick = onClick)
            .testTag("google_sign_in_button"),
        shape = RoundedCornerShape(27.dp),
        color = Color(0xFFF8FAFC),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF0F172A),
                    strokeWidth = 2.5.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Google 'G' Vector Logo
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Subtle vertical divider bar
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(Color(0xFFCBD5E1))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Sign in with Google",
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp
                    )
                }
            }
        }
    }
}

/**
 * Subtle decorative background elements echoing the dark tech diagonal accents in the mockup.
 */
@Composable
private fun DecorativeTechAccents() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top-left subtle diagonal glow
        Box(
            modifier = Modifier
                .size(260.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            TechAccentDeepBlue.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopStart)
        )

        // Bottom-right subtle diagonal glow
        Box(
            modifier = Modifier
                .size(280.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            TechAccentCyan.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.BottomEnd)
        )
    }
}
