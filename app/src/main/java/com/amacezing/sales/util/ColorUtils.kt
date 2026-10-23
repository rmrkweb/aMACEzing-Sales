package com.amacezing.sales.util

import kotlin.math.abs
import kotlin.math.roundToInt

/** Pure colour helpers for the business colour picker (hex "#RRGGBB" <-> HSV). */
object ColorUtils {
    private val HEX = Regex("^#[0-9A-Fa-f]{6}$")

    fun isValidHex(value: String): Boolean = HEX.matches(value.trim())

    /** Returns [hue 0..360, saturation 0..1, value 0..1]. */
    fun hexToHsv(hex: String): FloatArray {
        val clean = hex.trim().removePrefix("#")
        val rgb = clean.toLongOrNull(16) ?: return floatArrayOf(0f, 0f, 0.6f)
        val r = ((rgb shr 16) and 0xFF) / 255f
        val g = ((rgb shr 8) and 0xFF) / 255f
        val b = (rgb and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        val hue = when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta) % 6f)
            max == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }.let { if (it < 0f) it + 360f else it }
        val sat = if (max == 0f) 0f else delta / max
        return floatArrayOf(hue, sat, max)
    }

    fun hsvToHex(hue: Float, saturation: Float, value: Float): String {
        val h = ((hue % 360f) + 360f) % 360f
        val s = saturation.coerceIn(0f, 1f)
        val v = value.coerceIn(0f, 1f)
        val c = v * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = v - c
        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun channel(f: Float) = ((f + m) * 255f).roundToInt().coerceIn(0, 255)
        return "#%02X%02X%02X".format(channel(r1), channel(g1), channel(b1))
    }
}
