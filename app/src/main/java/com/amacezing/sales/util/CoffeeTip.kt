package com.amacezing.sales.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.amacezing.sales.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Where the "Buy me a coffee" QR image lives. It is read from the GitHub repository (not bundled in the
 * app), so replacing [IMAGE_PATH] on [BRANCH] updates the QR shown in the app without a new build.
 */
object CoffeeTip {
    const val BRANCH = "main"
    const val IMAGE_PATH = "tip/coffee-tip.png"
    private const val ALBUM = "aMACEzing"
    private const val SAVED_NAME_BASE = "amacezing-coffee-tip-qr"

    val imageUrl: String
        get() = "https://raw.githubusercontent.com/${BuildConfig.GITHUB_REPO}/$BRANCH/$IMAGE_PATH"

    /** The app never reuses a cached copy of the QR older than this (GitHub's own CDN also caches ~5 min). */
    const val CACHE_MAX_MINUTES = 5L

    /**
     * Image cache key that changes every [CACHE_MAX_MINUTES]. Using it as the memory and disk cache key
     * means a copy fetched in an earlier window is ignored, so the QR is re-downloaded at most 5 minutes late.
     */
    fun cacheKey(nowMillis: Long = System.currentTimeMillis()): String =
        "$imageUrl#${nowMillis / (CACHE_MAX_MINUTES * 60_000L)}"

    /** Saving into Pictures without a storage permission needs MediaStore scoped storage (Android 10+). */
    val canSaveToGallery: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private val client = OkHttpClient()

    /**
     * Downloads the current QR image and saves it to Pictures/aMACEzing. A copy saved earlier is replaced so
     * repeated downloads don't pile up. Returns the saved image's Uri, or null if it couldn't be saved.
     */
    suspend fun saveToGallery(context: Context): Uri? = withContext(Dispatchers.IO) {
        if (!canSaveToGallery) return@withContext null
        try {
            var bytes: ByteArray? = null
            var mime = "image/png"
            client.newCall(Request.Builder().url(imageUrl).build()).execute().use { response ->
                if (response.isSuccessful) {
                    bytes = response.body?.bytes()
                    response.body?.contentType()?.toString()?.substringBefore(';')
                        ?.takeIf { it.startsWith("image/") }?.let { mime = it }
                }
            }
            val data = bytes?.takeIf { it.isNotEmpty() } ?: return@withContext null

            val ext = when (mime) {
                "image/jpeg" -> "jpg"
                "image/webp" -> "webp"
                else -> "png"
            }
            val fileName = "$SAVED_NAME_BASE.$ext"
            val relativePath = "${Environment.DIRECTORY_PICTURES}/$ALBUM"
            val resolver = context.contentResolver
            val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            resolver.query(
                collection,
                arrayOf(MediaStore.Images.Media._ID),
                "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ? AND ${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?",
                arrayOf("$SAVED_NAME_BASE.%", "$relativePath%"),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    resolver.delete(ContentUris.withAppendedId(collection, cursor.getLong(0)), null, null)
                }
            }

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: return@withContext null
            try {
                val out = resolver.openOutputStream(uri) ?: throw IllegalStateException("No output stream")
                out.use { it.write(data) }
                resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
                uri
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                null
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
}
