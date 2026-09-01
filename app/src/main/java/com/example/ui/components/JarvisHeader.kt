package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.JarvisState
import com.example.ui.theme.JarvisAccentGreen
import com.example.ui.theme.JarvisAccentRed
import com.example.ui.theme.JarvisBorderColor
import com.example.ui.theme.JarvisCardSurface
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanLight
import com.example.ui.theme.JarvisTextDim
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun JarvisHeader(
    state: JarvisState,
    isSpeaking: Boolean,
    hasMessages: Boolean,
    modelSource: String,
    onStopSpeaking: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        JarvisCardSurface.copy(alpha = 0.95f),
                        JarvisCardSurface.copy(alpha = 0.6f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(JarvisBorderColor, Color.Transparent)
                ),
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // JARVIS Title & Status Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Glowing Arc Core Indicator
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                if (isSpeaking) JarvisAccentGreen.copy(alpha = 0.4f) else JarvisCyan.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isSpeaking) JarvisAccentGreen else JarvisCyan,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isSpeaking) JarvisAccentGreen else JarvisCyanLight)
                )
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.jarvis_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 3.sp,
                        color = JarvisCyanLight
                    )
                    Text(
                        text = "v1.0",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisTextDim
                    )
                }

                Text(
                    text = when (state) {
                        is JarvisState.Listening -> "AUDIO INPUT ACTIVE"
                        is JarvisState.Processing -> "NEURAL LINK COMPUTING"
                        is JarvisState.Speaking -> "SYNTHESIZING SPEECH"
                        is JarvisState.Error -> "SYSTEM ALERT"
                        is JarvisState.Idle -> "STANDBY // EN • HI • HINGLISH"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    color = when (state) {
                        is JarvisState.Listening -> JarvisCyan
                        is JarvisState.Processing -> JarvisCyanLight
                        is JarvisState.Speaking -> JarvisAccentGreen
                        is JarvisState.Error -> JarvisAccentRed
                        is JarvisState.Idle -> JarvisTextSecondary
                    }
                )
            }
        }

        // Action Buttons: Stop Speaking & Clear Conversation
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Stop Speaking Button (visible when JARVIS is actively speaking)
            AnimatedVisibility(
                visible = isSpeaking,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FilledTonalIconButton(
                    onClick = onStopSpeaking,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("stop_speaking_button"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = JarvisAccentRed.copy(alpha = 0.2f),
                        contentColor = JarvisAccentRed
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = stringResource(R.string.stop_speaking_desc),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Clear Conversation Button (visible when messages exist)
            AnimatedVisibility(
                visible = hasMessages,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FilledTonalIconButton(
                    onClick = onClearHistory,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("clear_history_button"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = JarvisCardSurface,
                        contentColor = JarvisTextSecondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = stringResource(R.string.clear_history_desc),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
