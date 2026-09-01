package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisAccentGreen
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanLight

@Composable
fun JarvisWaveformIndicator(
    isActive: Boolean,
    rmsLevel: Float = 0f,
    isSpeaking: Boolean = false,
    barCount: Int = 12,
    height: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

    Row(
        modifier = modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            val phaseDelay = (index * 120) % 800
            val animatedFactor by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 400 + (index * 50) % 300, delayMillis = phaseDelay, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )

            val dynamicHeightFraction = when {
                isSpeaking -> (0.25f + 0.75f * animatedFactor).coerceIn(0.15f, 1f)
                isActive -> {
                    // Reactive to microphone rms audio level
                    val base = 0.15f + (rmsLevel * 0.85f)
                    val jitter = (animatedFactor * 0.3f) * rmsLevel
                    (base + jitter).coerceIn(0.15f, 1f)
                }
                else -> 0.12f
            }

            val barBrush = when {
                isSpeaking -> Brush.verticalGradient(
                    listOf(JarvisAccentGreen, JarvisCyan)
                )
                isActive -> Brush.verticalGradient(
                    listOf(JarvisCyanLight, JarvisCyan)
                )
                else -> Brush.verticalGradient(
                    listOf(Color(0xFF1E3A5F), Color(0xFF0E223D))
                )
            }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(dynamicHeightFraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barBrush)
            )
        }
    }
}
