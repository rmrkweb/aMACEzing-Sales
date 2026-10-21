package com.amacezing.sales.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Packs the business logo into a small base64 PNG so it can live inside the Firestore
 * business-profile document (no Storage bucket needed) and be restored after logout / on a new device.
 */
object LogoCodec {
    private const val MAX_BASE64_CHARS = 600_000 // stays well under Firestore's 1 MiB document limit

    fun encodeFileToBase64(path: String): String {
        if (path.isBlank()) return ""
        val file = File(path)
        if (!file.exists()) return ""
        return try {
            val original = BitmapFactory.decodeFile(path) ?: return ""
            for (maxDim in listOf(512, 384, 256, 160, 96)) {
                val scale = minOf(1f, maxDim.toFloat() / maxOf(original.width, original.height))
                val bmp = if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        original,
                        (original.width * scale).toInt().coerceAtLeast(1),
                        (original.height * scale).toInt().coerceAtLeast(1),
                        true
                    )
                } else original
                val out = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                if (b64.length <= MAX_BASE64_CHARS) return b64
            }
            ""
        } catch (_: Exception) {
            ""
        }
    }

    /** Writes the logo to app storage and returns its path, or null if it could not be decoded. */
    fun decodeBase64ToFile(context: Context, base64: String): String? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            if (BitmapFactory.decodeByteArray(bytes, 0, bytes.size) == null) return null
            val dir = File(context.filesDir, "branding").apply { mkdirs() }
            val file = File(dir, "business_logo.png")
            file.writeBytes(bytes)
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }
}
