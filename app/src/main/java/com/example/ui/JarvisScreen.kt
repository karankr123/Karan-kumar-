package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.JarvisState
import com.example.ui.components.JarvisArcReactorMicButton
import com.example.ui.components.JarvisConversationView
import com.example.ui.components.JarvisHeader
import com.example.ui.components.JarvisQuickPrompts
import com.example.ui.components.JarvisStatusBanner
import com.example.ui.theme.JarvisBorderColor
import com.example.ui.theme.JarvisCardSurface
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanLight
import com.example.ui.theme.JarvisDarkBackground
import com.example.ui.theme.JarvisTextDim
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.viewmodel.JarvisViewModel

@Composable
fun JarvisScreen(
    viewModel: JarvisViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val state by viewModel.state.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val partialText by viewModel.partialText.collectAsStateWithLifecycle()
    val rmsLevel by viewModel.rmsLevel.collectAsStateWithLifecycle()
    val modelSource by viewModel.lastModelSource.collectAsStateWithLifecycle()

    var textInputValue by remember { mutableStateOf("") }
    var showTextInput by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    var isPermissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isPermissionDenied = !isGranted
        if (isGranted) {
            viewModel.onMicTapped(hasPermission = true, requestPermission = {})
        } else {
            Toast.makeText(
                context,
                "Microphone permission is required for voice recognition",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun handleMicTap() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            isPermissionDenied = false
            viewModel.onMicTapped(hasPermission = true, requestPermission = {})
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Auto-scroll to bottom when messages update
    LaunchedEffect(messages.size, partialText) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisDarkBackground)
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.navigationBars).union(WindowInsets.ime)),
        containerColor = JarvisDarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Futuristic HUD Header
            JarvisHeader(
                state = state,
                isSpeaking = state is JarvisState.Speaking,
                hasMessages = messages.isNotEmpty(),
                modelSource = modelSource,
                onStopSpeaking = { viewModel.stopSpeaking() },
                onClearHistory = { viewModel.clearConversation() }
            )

            // 2. Status Banner (Status, Listening / Speaking Indicator, Alerts)
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                JarvisStatusBanner(
                    state = state,
                    partialText = partialText,
                    rmsLevel = rmsLevel,
                    onStopSpeaking = { viewModel.stopSpeaking() },
                    onDismissError = { viewModel.resetStateToIdle() }
                )
            }

            // Permission Recovery Banner (if microphone permission was denied)
            AnimatedVisibility(
                visible = isPermissionDenied,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(com.example.ui.theme.JarvisAccentRed.copy(alpha = 0.15f))
                        .border(1.dp, com.example.ui.theme.JarvisAccentRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Mic Permission",
                            tint = com.example.ui.theme.JarvisAccentRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Microphone permission required for voice recognition.",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisTextPrimary
                        )
                    }

                    androidx.compose.material3.TextButton(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                    ) {
                        Text(
                            text = "ALLOW",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisCyan
                        )
                    }
                }
            }

            // 3. Conversation Stream Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty() && partialText.isEmpty()) {
                    // Empty placeholder HUD
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "JARVIS NEURAL CORE INITIALIZED",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp,
                            color = JarvisCyan.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the microphone below and speak in English, Hindi, or Hinglish.",
                            fontSize = 13.sp,
                            color = JarvisTextSecondary,
                            fontFamily = FontFamily.Monospace,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    JarvisConversationView(
                        messages = messages,
                        listState = listState,
                        onReplayMessage = { msg -> viewModel.replayMessage(msg) }
                    )
                }
            }

            // 4. Quick Prompt Suggestions
            JarvisQuickPrompts(
                onSelectPrompt = { promptText ->
                    viewModel.sendUserMessage(promptText)
                }
            )

            // 5. Text Input toggle section (for typing fallback & testing)
            AnimatedVisibility(
                visible = showTextInput,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInputValue,
                        onValueChange = { textInputValue = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("text_input_field"),
                        placeholder = {
                            Text(
                                "Type command (English/Hindi/Hinglish)...",
                                color = JarvisTextDim,
                                fontSize = 13.sp
                            )
                        },
                        textStyle = TextStyle(
                            color = JarvisTextPrimary,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisBorderColor,
                            focusedContainerColor = JarvisCardSurface,
                            unfocusedContainerColor = JarvisCardSurface
                        ),
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textInputValue.isNotBlank()) {
                                    viewModel.sendUserMessage(textInputValue)
                                    textInputValue = ""
                                    focusManager.clearFocus()
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (textInputValue.isNotBlank()) {
                                viewModel.sendUserMessage(textInputValue)
                                textInputValue = ""
                                focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(JarvisCyan, Color(0xFF0090B0)))
                            )
                            .testTag("send_text_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send text message",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 6. Central Arc Reactor Microphone Controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Center large microphone button
                JarvisArcReactorMicButton(
                    state = state,
                    rmsLevel = rmsLevel,
                    onClick = { handleMicTap() },
                    size = 140.dp
                )

                // Keyboard toggle button on the right side
                IconButton(
                    onClick = { showTextInput = !showTextInput },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(JarvisCardSurface)
                        .border(1.dp, JarvisBorderColor, CircleShape)
                        .testTag("toggle_keyboard_button")
                ) {
                    Icon(
                        imageVector = if (showTextInput) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                        contentDescription = "Toggle Keyboard Input",
                        tint = if (showTextInput) JarvisCyan else JarvisTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
