package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisBorderColor
import com.example.ui.theme.JarvisCardSurface
import com.example.ui.theme.JarvisCyanLight
import com.example.ui.theme.JarvisTextPrimary

@Composable
fun JarvisQuickPrompts(
    onSelectPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prompts = listOf(
        "Hello JARVIS",
        "Kaise ho JARVIS?",
        "What is your status?",
        "Kya chal raha hai?",
        "What can you do?",
        "Tell me a fun fact",
        "What is the time?"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        prompts.forEachIndexed { index, prompt ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(JarvisCardSurface)
                    .border(1.dp, JarvisBorderColor, RoundedCornerShape(20.dp))
                    .clickable { onSelectPrompt(prompt) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("quick_prompt_$index")
            ) {
                Text(
                    text = prompt,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = JarvisTextPrimary
                )
            }
        }
    }
}
