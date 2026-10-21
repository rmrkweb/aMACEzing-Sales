package com.amacezing.sales.security

import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Device-local setting (never synced to the cloud). */
object SecurityPrefs {
    private const val FILE = "app_security_prefs"
    private const val KEY_BIOMETRIC_LOCK = "biometric_lock_enabled"

    fun isBiometricLockEnabled(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_BIOMETRIC_LOCK, false)

    fun setBiometricLockEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_BIOMETRIC_LOCK, enabled).apply()
    }
}

/** Holds whether the app is currently locked behind biometric / device credential. */
object AppLock {
    /** Re-lock only if the app was in the background longer than this. */
    private const val GRACE_MS = 60_000L

    var isLocked by mutableStateOf(false)
        private set

    private var initialized = false
    private var stoppedAt = 0L

    /** Called once per process (cold start), so rotation does not re-lock. */
    fun initOnce(context: Context) {
        if (initialized) return
        initialized = true
        isLocked = SecurityPrefs.isBiometricLockEnabled(context)
    }

    fun onActivityStopped() {
        stoppedAt = SystemClock.elapsedRealtime()
    }

    fun onActivityStarted(context: Context) {
        if (stoppedAt != 0L &&
            SecurityPrefs.isBiometricLockEnabled(context) &&
            SystemClock.elapsedRealtime() - stoppedAt > GRACE_MS
        ) {
            isLocked = true
        }
    }

    fun unlock() {
        isLocked = false
    }
}

object BiometricAuth {
    // Strong-only + device credential is not supported before Android 11, so allow weak + credential.
    private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    fun canAuthenticate(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    // Tracks which activity instance has a prompt open, so a rotated (recreated) activity can re-attach.
    private var promptActivity: FragmentActivity? = null

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (promptActivity === activity) return
        promptActivity = activity

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    promptActivity = null
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    promptActivity = null
                    onFailure(errString.toString())
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply { if (subtitle != null) setSubtitle(subtitle) }
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()

        prompt.authenticate(info)
    }
}

fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
