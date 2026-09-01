package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.service.JarvisAiService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("JARVIS", appName)
  }

  @Test
  fun `test jarvis response to hello in English`() = runBlocking {
    val aiService = JarvisAiService()
    val response = aiService.getResponse("Hello JARVIS")
    assertTrue(response.replyText.contains("JARVIS", ignoreCase = true) || response.replyText.contains("sir", ignoreCase = true))
  }

  @Test
  fun `test jarvis response to Hindi and Hinglish greetings`() = runBlocking {
    val aiService = JarvisAiService()
    val hindiResponse = aiService.getResponse("Kaise ho JARVIS?")
    assertTrue(hindiResponse.replyText.isNotBlank())

    val namasteResponse = aiService.getResponse("Namaste JARVIS")
    assertTrue(namasteResponse.replyText.contains("sahayata", ignoreCase = true) || namasteResponse.replyText.contains("Namaste", ignoreCase = true))
  }
}


