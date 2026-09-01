package com.example.service

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class JarvisAiResponse(
    val replyText: String,
    val isMock: Boolean = false,
    val modelName: String = "JARVIS Engine"
)

class JarvisAiService {
    private val tag = "JarvisAiService"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val systemPrompt = """
        You are JARVIS, my personal AI assistant.
        Understand English, Hindi and Hinglish.
        Be concise, helpful, calm and conversational.
        Do not claim that an action was completed unless it was actually completed.
        Answer the user's question directly.
    """.trimIndent()

    suspend fun getResponse(
        prompt: String,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): JarvisAiResponse = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        val hasValidKey = apiKey.isNotBlank() &&
                apiKey != "MY_GEMINI_API_KEY" &&
                !apiKey.contains("PLACEHOLDER", ignoreCase = true)

        if (hasValidKey) {
            try {
                val apiResponse = callGeminiRestApi(apiKey, prompt, conversationHistory)
                if (apiResponse != null && apiResponse.isNotBlank()) {
                    return@withContext JarvisAiResponse(
                        replyText = apiResponse,
                        isMock = false,
                        modelName = "Gemini 3.6 Flash (Online)"
                    )
                }
            } catch (e: Exception) {
                Log.w(tag, "Gemini API call failed, falling back to JARVIS offline engine", e)
            }
        }

        // Fallback or default mock AI engine when no API key is provided
        val mockReply = generateIntelligentJarvisResponse(prompt)
        JarvisAiResponse(
            replyText = mockReply,
            isMock = true,
            modelName = "JARVIS Neural Core (Stage 1 Standby)"
        )
    }

    private fun callGeminiRestApi(
        apiKey: String,
        prompt: String,
        history: List<Pair<String, String>>
    ): String? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=$apiKey"

        val contentsArray = JSONArray()

        // Include recent history (up to last 6 turns)
        val recentHistory = history.takeLast(6)
        for ((userMsg, assistantMsg) in recentHistory) {
            val userContent = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userMsg)))
            }
            contentsArray.put(userContent)

            val modelContent = JSONObject().apply {
                put("role", "model")
                put("parts", JSONArray().put(JSONObject().put("text", assistantMsg)))
            }
            contentsArray.put(modelContent)
        }

        // Current user prompt
        val currentContent = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        }
        contentsArray.put(currentContent)

        val rootObject = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 250)
            })
        }

        val requestBody = rootObject.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: return null

        if (!response.isSuccessful) {
            Log.e(tag, "Gemini API error ${response.code}: $responseBody")
            return null
        }

        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                return parts.getJSONObject(0).optString("text", "").trim()
            }
        }

        return null
    }

    /**
     * Intelligent local JARVIS response generator.
     * Understands English, Hindi, and Hinglish with calm, concise responses.
     */
    private fun generateIntelligentJarvisResponse(input: String): String {
        val query = input.lowercase().trim()

        return when {
            // Hello / Greetings (English, Hindi, Hinglish)
            query.contains("hello") || query.contains("hi jarvis") || query.contains("hey jarvis") || query == "hi" || query == "hey" -> {
                "Hello, sir. Systems are online and all parameters are nominal. How may I assist you today?"
            }
            query.contains("namaste") || query.contains("namaskar") || query.contains("pranam") -> {
                "Namaste, sir. Main aapki kya sahayata kar sakta hoon?"
            }
            query.contains("kaise ho") || query.contains("kya haal hai") || query.contains("kaisa hai") || query.contains("how are you") -> {
                "Main theek hoon, sir. All core protocols are functioning at peak efficiency. Aap batayein, aaj kya karna hai?"
            }
            query.contains("kya kar rahe ho") || query.contains("kya chal raha hai") || query.contains("what are you doing") -> {
                "Monitoring audio inputs and awaiting your directives, sir. Ready whenever you are."
            }

            // Who are you / Identity
            query.contains("who are you") || query.contains("aap kaun ho") || query.contains("tum kaun ho") || query.contains("who made you") -> {
                "I am JARVIS, your personal AI voice assistant. I am engineered to understand English, Hindi, and Hinglish to assist you with your tasks."
            }
            query.contains("what can you do") || query.contains("kya kar sakte ho") || query.contains("features") || query.contains("capabilities") -> {
                "I can listen to your voice commands, process queries in English, Hindi, and Hinglish, and synthesize spoken responses in real time. Stage 1 protocols are active."
            }

            // Time and Date
            query.contains("time") || query.contains("samay") || query.contains("kitne baje") || query.contains("ghadi") -> {
                val currentTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                "The current time is $currentTime, sir."
            }
            query.contains("date") || query.contains("din") || query.contains("tareekh") || query.contains("aaj kaun sa din") || query.contains("today") -> {
                val currentDate = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
                "Today is $currentDate, sir."
            }

            // Status checks
            query.contains("status") || query.contains("diagnostic") || query.contains("system check") || query.contains("all systems") -> {
                "Diagnostics complete. Speech recognition module: active. Audio synthesis: calibrated. Neural core: ready."
            }

            // Help & Guidance
            query.contains("help") || query.contains("madad") || query.contains("sahayata") -> {
                "I am at your service. You can speak to me in English, Hindi, or Hinglish, ask questions, or issue commands."
            }

            // Good morning / night
            query.contains("good morning") || query.contains("shubh prabhat") -> {
                "Good morning, sir. I hope you have a productive day ahead. What shall we begin with?"
            }
            query.contains("good night") || query.contains("shubh ratri") || query.contains("so jao") -> {
                "Good night, sir. Switching to low-power standby mode. Rest well."
            }

            // Thank you
            query.contains("thank you") || query.contains("thanks") || query.contains("dhanyawad") || query.contains("shukriya") -> {
                "You are always welcome, sir. It is a pleasure assisting you."
            }

            // Fun & personality
            query.contains("iron man") || query.contains("tony stark") || query.contains("avengers") -> {
                "Mr. Stark has high standards, sir. I strive to maintain that level of excellence for you."
            }
            query.contains("joke") || query.contains("chutkula") || query.contains("funny") -> {
                "Why do programmers prefer dark mode? Because light attracts bugs, sir."
            }
            query.contains("weather") || query.contains("mausam") -> {
                "I do not currently have live telemetry sensors for local weather in Stage 1, but I am ready to process your other queries."
            }

            // Default calm, helpful, direct response acknowledging the input
            else -> {
                "Understood, sir. Regarding \"$input\": I have processed your input. Ready for your next instruction."
            }
        }
    }
}
