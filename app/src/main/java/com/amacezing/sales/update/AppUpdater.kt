package com.amacezing.sales.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.amacezing.sales.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val apkUrl: String,
    val sizeBytes: Long,
    val notes: String
)

sealed interface CheckResult {
    data object Disabled : CheckResult
    data object UpToDate : CheckResult
    data class Available(val info: UpdateInfo) : CheckResult
    data class Error(val message: String) : CheckResult
}

/**
 * Checks the public GitHub repo's latest Release and installs its APK.
 * Release tags look like "v1.2.0-b7" where "b7" is the versionCode (see android-release.yml).
 */
object AppUpdater {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val buildRegex = Regex("-b(\\d+)$")

    suspend fun check(): CheckResult = withContext(Dispatchers.IO) {
        val repo = BuildConfig.GITHUB_REPO
        if (repo.isBlank()) return@withContext CheckResult.Disabled
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$repo/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "aMACEzing-Sales-Updater")
                .build()
            client.newCall(request).execute().use { resp ->
                if (resp.code == 404) return@withContext CheckResult.UpToDate // no releases yet
                if (!resp.isSuccessful) {
                    return@withContext CheckResult.Error("GitHub returned HTTP ${resp.code}")
                }
                val json = JSONObject(resp.body?.string().orEmpty())
                val tag = json.optString("tag_name")
                val match = buildRegex.find(tag)
                val code = match?.groupValues?.get(1)?.toIntOrNull()
                val cleanVersion = tag.removePrefix("v").replace(buildRegex, "").trim()

                val isNewer = if (code != null) {
                    code > BuildConfig.VERSION_CODE
                } else {
                    cleanVersion != BuildConfig.VERSION_NAME
                }
                if (!isNewer) return@withContext CheckResult.UpToDate

                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null
                var size = 0L
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                            apkUrl = a.optString("browser_download_url")
                            size = a.optLong("size")
                            break
                        }
                    }
                }
                if (apkUrl.isNullOrBlank()) {
                    return@withContext CheckResult.Error("Release has no APK attached")
                }
                CheckResult.Available(
                    UpdateInfo(
                        versionName = cleanVersion,
                        versionCode = code ?: (BuildConfig.VERSION_CODE + 1),
                        apkUrl = apkUrl,
                        sizeBytes = size,
                        notes = json.optString("body").trim()
                    )
                )
            }
        } catch (e: Exception) {
            CheckResult.Error(e.message ?: "Could not check for updates")
        }
    }

    /** Downloads the APK into the app cache and verifies it belongs to this app. */
    suspend fun download(
        context: Context,
        info: UpdateInfo,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val uri = Uri.parse(info.apkUrl)
        if (uri.scheme != "https" || uri.host != "github.com") {
            throw IOException("Refusing to download from an unexpected address")
        }
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "update.apk")

        val request = Request.Builder().url(info.apkUrl)
            .header("User-Agent", "aMACEzing-Sales-Updater").build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Download failed (HTTP ${resp.code})")
            val body = resp.body ?: throw IOException("Empty download")
            val total = body.contentLength().takeIf { it > 0 } ?: info.sizeBytes
            var done = 0L
            var lastPercent = -1
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buf = ByteArray(16 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        done += n
                        if (total > 0) {
                            val percent = (done * 100 / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(done.toFloat() / total)
                            }
                        }
                    }
                }
            }
        }

        // Sanity check: must be this app and newer than what is installed.
        val pm = context.packageManager
        val archive = pm.getPackageArchiveInfo(target.absolutePath, 0)
        val installed = pm.getPackageInfo(context.packageName, 0)
        if (archive == null || archive.packageName != context.packageName ||
            PackageInfoCompat.getLongVersionCode(archive) <= PackageInfoCompat.getLongVersionCode(installed)
        ) {
            target.delete()
            throw IOException("Downloaded file is not a valid update for this app")
        }
        target
    }

    /** Returns false if the user must first allow installs from this app. */
    fun install(context: Context, apk: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updatefiles", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return true
    }
}
