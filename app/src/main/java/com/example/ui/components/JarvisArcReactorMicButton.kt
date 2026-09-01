package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.JarvisState
import com.example.ui.theme.JarvisAccentGreen
import com.example.ui.theme.JarvisAccentRed
import com.example.ui.theme.JarvisBorderColor
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanLight
import com.example.ui.theme.JarvisDarkBackground
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JarvisArcReactorMicButton(
    state: JarvisState,
    rmsLevel: Float = 0f,
    onClick: () -> Unit,
    size: Dp = 150.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc_reactor_anim")

    // Slow rotation for ambient HUD feel
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state is JarvisState.Listening || state is JarvisState.Processing) 4000 else 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Reverse rotation for outer segments
    val counterRotationAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state is JarvisState.Listening) 6000 else 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_rotation"
    )

    // Pulsing effect when active
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (state is JarvisState.Listening) 1.08f + (rmsLevel * 0.12f) else if (state is JarvisState.Speaking) 1.05f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state is JarvisState.Listening) 700 else 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (state is JarvisState.Listening) 0.9f else if (state is JarvisState.Speaking) 0.75f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val primaryColor = when (state) {
        is JarvisState.Listening -> JarvisCyan
        is JarvisState.Processing -> JarvisCyanLight
        is JarvisState.Speaking -> JarvisAccentGreen
        is JarvisState.Error -> JarvisAccentRed
        is JarvisState.Idle -> JarvisCyan
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(pulseScale)
            .testTag("mic_button"),
        contentAlignment = Alignment.Center
    ) {
        // Futuristic Canvas Arc Reactor HUD Graphics
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val maxRadius = this.size.minDimension / 2

            // 1. Ambient outer glow circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = glowAlpha * 0.35f),
                        primaryColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius,
                center = center
            )

            // 2. Outermost dashed HUD tech ring (counter-rotating)
            drawCircle(
                color = primaryColor.copy(alpha = 0.4f),
                radius = maxRadius * 0.94f,
                center = center,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 16f), counterRotationAngle)
                )
            )

            // 3. Middle segmented ring (rotating)
            val segmentRadius = maxRadius * 0.82f
            val segmentCount = 8
            val segmentArcAngle = 360f / segmentCount
            for (i in 0 until segmentCount) {
                val startAngle = rotationAngle + (i * segmentArcAngle) + 4f
                val sweepAngle = segmentArcAngle - 8f
                drawArc(
                    color = if (i % 2 == 0) primaryColor.copy(alpha = 0.85f) else primaryColor.copy(alpha = 0.35f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - segmentRadius, center.y - segmentRadius),
                    size = androidx.compose.ui.geometry.Size(segmentRadius * 2, segmentRadius * 2),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 4. Arc Reactor Core Ticks (12 directional tick marks)
            val tickRadiusInner = maxRadius * 0.68f
            val tickRadiusOuter = maxRadius * 0.74f
            for (i in 0 until 12) {
                val angleRad = Math.toRadians((i * 30 + rotationAngle).toDouble())
                val startPoint = Offset(
                    center.x + (tickRadiusInner * cos(angleRad)).toFloat(),
                    center.y + (tickRadiusInner * sin(angleRad)).toFloat()
                )
                val endPoint = Offset(
                    center.x + (tickRadiusOuter * cos(angleRad)).toFloat(),
                    center.y + (tickRadiusOuter * sin(angleRad)).toFloat()
                )
                drawLine(
                    color = primaryColor.copy(alpha = 0.6f),
                    start = startPoint,
                    end = endPoint,
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Inner Interactive Circular Button Core
        Box(
            modifier = Modifier
                .size(size * 0.62f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            when (state) {
                                is JarvisState.Listening -> Color(0xFF00384D)
                                is JarvisState.Speaking -> Color(0xFF003D29)
                                is JarvisState.Error -> Color(0xFF4A0E17)
                                else -> Color(0xFF081938)
                            },
                            JarvisDarkBackground
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primaryColor,
                            primaryColor.copy(alpha = 0.4f),
                            primaryColor
                        )
                    ),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = primaryColor, radius = size / 2),
                    onClick = onClick
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            val icon = when (state) {
                is JarvisState.Listening -> Icons.Default.GraphicEq
                is JarvisState.Processing -> Icons.Default.HourglassTop
                is JarvisState.Speaking -> Icons.AutoMirrored.Filled.VolumeUp
                is JarvisState.Error -> Icons.Default.MicNone
                is JarvisState.Idle -> Icons.Default.Mic
            }

            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.mic_button_desc),
                tint = primaryColor,
                modifier = Modifier.size(size * 0.28f)
            )
        }
    }
}
