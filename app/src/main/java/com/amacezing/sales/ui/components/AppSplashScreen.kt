package com.amacezing.sales.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amacezing.sales.R

private val SplashBackground = Color(0xFF050B17)
private val SplashCyan = Color(0xFF00B8F0)
private val SplashViolet = Color(0xFF7C4DFF)
private val SplashTagline = Color(0xFF8E9ACB)
private val SplashStatus = Color(0xFF7F8AB0)

/**
 * Branded splash used on app start and as the backdrop for the biometric unlock.
 * The logo sits in a pure-black container so it blends with the artwork.
 */
@Composable
fun AppSplashScreen(
    modifier: Modifier = Modifier,
    statusText: String = "Loading...",
    showSpinner: Boolean = true,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBackground)
            .testTag("app_splash_screen")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) { drawCornerGlows() }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1.1f))

            // Logo (black container so the image blends in)
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.amacezing_logo),
                    contentDescription = "aMACEzing logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "aMACEzing",
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Sales",
                color = SplashCyan,
                fontSize = 44.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Manage Your Business. Grow Your Sales.",
                color = SplashTagline,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(0.9f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showSpinner) GradientSpinner()
                Text(text = statusText, color = SplashStatus, fontSize = 14.sp, textAlign = TextAlign.Center)
                if (actionLabel != null && onAction != null) {
                    Button(
                        onClick = onAction,
                        colors = ButtonDefaults.buttonColors(containerColor = SplashCyan, contentColor = Color.Black),
                        modifier = Modifier.testTag("splash_unlock_button")
                    ) {
                        Text(actionLabel, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1.1f))
        }
    }
}

@Composable
private fun GradientSpinner() {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "angle"
    )
    Canvas(modifier = Modifier.size(44.dp)) {
        val strokeWidth = 4.dp.toPx()
        val brush = Brush.sweepGradient(listOf(SplashCyan, SplashViolet, SplashCyan))
        rotate(angle.value) {
            drawArc(
                brush = brush,
                startAngle = 0f,
                sweepAngle = 290f,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

private fun DrawScope.drawCornerGlows() {
    val w = size.width
    val h = size.height

    // Soft radial glows in two opposite corners
    drawCircle(
        brush = Brush.radialGradient(listOf(SplashCyan.copy(alpha = 0.18f), Color.Transparent), center = Offset(0f, 0f), radius = w * 0.7f),
        radius = w * 0.7f, center = Offset(0f, 0f)
    )
    drawCircle(
        brush = Brush.radialGradient(listOf(SplashViolet.copy(alpha = 0.20f), Color.Transparent), center = Offset(w, h), radius = w * 0.7f),
        radius = w * 0.7f, center = Offset(w, h)
    )

    val brush = Brush.linearGradient(listOf(SplashCyan, SplashViolet), start = Offset(0f, 0f), end = Offset(w * 0.3f, h * 0.12f))
    val brushBottom = Brush.linearGradient(listOf(SplashViolet, SplashCyan), start = Offset(w * 0.7f, h * 0.88f), end = Offset(w, h))

    fun curve(x0: Float, y0: Float, cx: Float, cy: Float, x1: Float, y1: Float) = Path().apply {
        moveTo(x0, y0); quadraticTo(cx, cy, x1, y1)
    }

    // Top-left arcs
    val tl1 = curve(0f, h * 0.103f, w * 0.09f, h * 0.045f, w * 0.195f, 0f)
    val tl2 = curve(0f, h * 0.144f, w * 0.12f, h * 0.07f, w * 0.25f, 0f)
    drawPath(tl1, brush, style = Stroke(width = 10f, cap = StrokeCap.Round), alpha = 0.25f)
    drawPath(tl1, brush, style = Stroke(width = 3f, cap = StrokeCap.Round))
    drawPath(tl2, brush, style = Stroke(width = 10f, cap = StrokeCap.Round), alpha = 0.2f)
    drawPath(tl2, brush, style = Stroke(width = 3f, cap = StrokeCap.Round), alpha = 0.9f)

    // Bottom-right arcs
    val br1 = curve(w, h * 0.77f, w * 0.88f, h * 0.93f, w * 0.72f, h)
    val br2 = curve(w, h * 0.82f, w * 0.92f, h * 0.95f, w * 0.80f, h)
    drawPath(br1, brushBottom, style = Stroke(width = 10f, cap = StrokeCap.Round), alpha = 0.25f)
    drawPath(br1, brushBottom, style = Stroke(width = 3f, cap = StrokeCap.Round))
    drawPath(br2, brushBottom, style = Stroke(width = 10f, cap = StrokeCap.Round), alpha = 0.2f)
    drawPath(br2, brushBottom, style = Stroke(width = 3f, cap = StrokeCap.Round), alpha = 0.9f)
}
