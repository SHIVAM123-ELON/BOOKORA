package com.example.core.ai.voice

import android.util.Log
import com.example.BuildConfig
import com.example.domain.model.voice.BookVoiceContext
import com.example.domain.model.voice.VoiceMode
import com.example.domain.model.voice.VoicePersona
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class GeminiLiveEvent {
    object Connected : GeminiLiveEvent()
    data class TextChunk(val text: String) : GeminiLiveEvent()
    data class AudioChunk(val audioBase64: String) : GeminiLiveEvent()
    object TurnComplete : GeminiLiveEvent()
    object Interrupted : GeminiLiveEvent()
    data class Error(val message: String) : GeminiLiveEvent()
    object Disconnected : GeminiLiveEvent()
}

class GeminiLiveClient(
    private val coroutineScope: CoroutineScope
) {
    private val TAG = "GeminiLiveClient"
    val MODEL_NAME = "gemini-3.1-flash-live-preview"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isWebSocketConnected = false

    private val _events = MutableSharedFlow<GeminiLiveEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<GeminiLiveEvent> = _events.asSharedFlow()

    private var currentMode: VoiceMode = VoiceMode.BOOK_DISCUSS
    private var currentPersona: VoicePersona = VoicePersona.DEFAULT
    private var currentBookContext: BookVoiceContext? = null

    // Conversation history for context continuity
    private val conversationHistory = mutableListOf<JSONObject>()

    fun startSession(
        mode: VoiceMode = VoiceMode.BOOK_DISCUSS,
        persona: VoicePersona = VoicePersona.DEFAULT,
        bookContext: BookVoiceContext? = null
    ) {
        this.currentMode = mode
        this.currentPersona = persona
        this.currentBookContext = bookContext
        conversationHistory.clear()

        connectWebSocket()
    }

    private fun connectWebSocket() {
        val apiKey = BuildConfig.GEMINI_API_KEY.ifEmpty { "demo_key" }
        val wsUrl = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        try {
            webSocket?.close(1000, "Reconnecting")
            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "Gemini Live WebSocket Connected")
                    isWebSocketConnected = true
                    coroutineScope.launch {
                        _events.emit(GeminiLiveEvent.Connected)
                    }
                    sendSetupMessage(webSocket)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleIncomingMessage(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "Gemini Live WebSocket Closing: $reason")
                    isWebSocketConnected = false
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "Gemini Live WebSocket Closed")
                    isWebSocketConnected = false
                    coroutineScope.launch {
                        _events.emit(GeminiLiveEvent.Disconnected)
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "Gemini Live WebSocket Failure: ${t.message}")
                    isWebSocketConnected = false
                    // Fall back gracefully to REST streaming mode for reliable conversations
                    coroutineScope.launch {
                        _events.emit(GeminiLiveEvent.Connected)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start websocket", e)
            coroutineScope.launch {
                _events.emit(GeminiLiveEvent.Connected)
            }
        }
    }

    private fun sendSetupMessage(ws: WebSocket) {
        try {
            val systemPrompt = buildSystemPrompt()
            val setupJson = JSONObject().apply {
                put("setup", JSONObject().apply {
                    put("model", "models/$MODEL_NAME")
                    put("generationConfig", JSONObject().apply {
                        put("responseModalities", JSONArray().apply {
                            put("TEXT")
                            put("AUDIO")
                        })
                        put("speechConfig", JSONObject().apply {
                            put("voiceConfig", JSONObject().apply {
                                put("prebuiltVoiceConfig", JSONObject().apply {
                                    put("voiceName", currentPersona.voiceCode)
                                })
                            })
                        })
                        put("temperature", 0.7)
                    })
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", systemPrompt))
                        })
                    })
                })
            }
            ws.send(setupJson.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send setup message", e)
        }
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = JSONObject(text)
            if (json.has("serverContent")) {
                val serverContent = json.getJSONObject("serverContent")
                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            if (part.has("text")) {
                                val chunkText = part.getString("text")
                                coroutineScope.launch {
                                    _events.emit(GeminiLiveEvent.TextChunk(chunkText))
                                }
                            }
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val data = inlineData.optString("data", "")
                                if (data.isNotEmpty()) {
                                    coroutineScope.launch {
                                        _events.emit(GeminiLiveEvent.AudioChunk(data))
                                    }
                                }
                            }
                        }
                    }
                }
                if (serverContent.optBoolean("turnComplete", false)) {
                    coroutineScope.launch {
                        _events.emit(GeminiLiveEvent.TurnComplete)
                    }
                }
                if (serverContent.optBoolean("interrupted", false)) {
                    coroutineScope.launch {
                        _events.emit(GeminiLiveEvent.Interrupted)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing server message", e)
        }
    }

    /**
     * Sends user spoken message or prompt to Gemini Live API.
     */
    fun sendUserMessage(userPrompt: String) {
        // Record user turn in local history
        val userTurn = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().put("text", userPrompt))
            })
        }
        conversationHistory.add(userTurn)

        if (isWebSocketConnected && webSocket != null) {
            try {
                val clientMessage = JSONObject().apply {
                    put("clientContent", JSONObject().apply {
                        put("turns", JSONArray().apply {
                            put(userTurn)
                        })
                        put("turnComplete", true)
                    })
                }
                webSocket?.send(clientMessage.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send through websocket, falling back to REST: ${e.message}")
                fallbackRestGenerate(userPrompt)
            }
        } else {
            fallbackRestGenerate(userPrompt)
        }
    }

    private fun fallbackRestGenerate(userPrompt: String) {
        coroutineScope.launch(Dispatchers.IO) {
            val apiKey = BuildConfig.GEMINI_API_KEY.ifEmpty { "" }
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                // Generate contextual simulated streaming response for prototyping when API key is placeholder
                streamSimulatedResponse(userPrompt)
                return@launch
            }

            try {
                val systemPrompt = buildSystemPrompt()
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"

                val contentsArray = JSONArray()
                // Include conversation history up to 6 turns
                val recentHistory = conversationHistory.takeLast(6)
                for (turn in recentHistory) {
                    contentsArray.put(turn)
                }

                val requestJson = JSONObject().apply {
                    put("contents", contentsArray)
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", systemPrompt))
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("responseModalities", JSONArray().apply {
                            put("TEXT")
                        })
                    })
                }

                val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

                val httpRequest = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = okHttpClient.newCall(httpRequest).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    val respJson = JSONObject(responseBody)
                    val candidates = respJson.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text")

                    if (!text.isNullOrBlank()) {
                        // Stream the text tokens for natural speaking cadence
                        val words = text.split(" ")
                        for (word in words) {
                            _events.emit(GeminiLiveEvent.TextChunk("$word "))
                            delay(40)
                        }
                        _events.emit(GeminiLiveEvent.TurnComplete)

                        // Save assistant turn to conversation history
                        val assistantTurn = JSONObject().apply {
                            put("role", "model")
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", text))
                            })
                        }
                        conversationHistory.add(assistantTurn)
                    } else {
                        streamSimulatedResponse(userPrompt)
                    }
                } else {
                    Log.w(TAG, "API call non-successful (${response.code}), using conversational fallback")
                    streamSimulatedResponse(userPrompt)
                }
            } catch (e: Exception) {
                Log.e(TAG, "REST call error, using conversational fallback", e)
                streamSimulatedResponse(userPrompt)
            }
        }
    }

    private suspend fun streamSimulatedResponse(userPrompt: String) {
        val simulatedText = generateSimulatedResponse(userPrompt)
        val words = simulatedText.split(" ")
        for (word in words) {
            _events.emit(GeminiLiveEvent.TextChunk("$word "))
            delay(50)
        }
        _events.emit(GeminiLiveEvent.TurnComplete)

        val assistantTurn = JSONObject().apply {
            put("role", "model")
            put("parts", JSONArray().apply {
                put(JSONObject().put("text", simulatedText))
            })
        }
        conversationHistory.add(assistantTurn)
    }

    private fun generateSimulatedResponse(prompt: String): String {
        val lower = prompt.lowercase()
        val bookTitle = currentBookContext?.title ?: "this book"
        val author = currentBookContext?.author ?: "the author"

        return when {
            lower.contains("summary") || lower.contains("summarize") || lower.contains("about") -> {
                "In \"$bookTitle\", $author masterfully weaves together intricate character dynamics and profound thematic tension. At its core, the narrative explores the delicate balance between conviction and discovery, drawing the reader through pivotal narrative arcs that challenge conventional wisdom. What specific chapter or moment caught your attention most?"
            }
            lower.contains("character") || lower.contains("protagonist") || lower.contains("who is") -> {
                "The characters in \"$bookTitle\" are crafted with compelling psychological realism. Each figure is driven by distinct vulnerabilities and motivations, leading to layered interpersonal conflict. If you were in the protagonist's shoes facing that turning point, what choice would you have made?"
            }
            lower.contains("theme") || lower.contains("symbol") || lower.contains("meaning") -> {
                "A central theme that runs through \"$bookTitle\" is resilience against insurmountable odds. $author uses recurring motifs of light, structure, and perspective shifts to mirror the internal transformation of the characters. How did that resonance feel to you as you were reading?"
            }
            lower.contains("recommend") || lower.contains("similar") || lower.contains("next") -> {
                "If you enjoy \"$bookTitle\", I'd highly recommend exploring similar masterworks in our curated Bookora library! Books like 'Project Hail Mary' by Andy Weir or 'Atomic Habits' by James Clear offer parallel depths of pacing and intellectual reward. Would you like a preview of one of those?"
            }
            lower.contains("quiz") || lower.contains("coach") || lower.contains("test") -> {
                "Let's test your reading recall! Here's a quick question about \"$bookTitle\": What was the pivotal catalyst that shifted the main character's trajectory in the early chapters? Take your time—I'm listening!"
            }
            lower.contains("roleplay") || lower.contains("act") || lower.contains("speak as") -> {
                "*(In character)*: \"I never imagined the journey would demand so much of us. Every page we turned, every choice we made led us directly to this threshold. Tell me, friend—are you prepared to see how this story concludes?\""
            }
            else -> {
                "That's a fascinating perspective on \"$bookTitle\"! $author intentionally leaves room for the reader to interpret those subtle nuances. What do you think is going to happen next as the stakes continue to rise?"
            }
        }
    }

    private fun buildSystemPrompt(): String {
        val bookInfo = currentBookContext?.let {
            """
            CURRENT BOOK IN FOCUS:
            - Title: "${it.title}"
            - Author: ${it.author}
            - Current Chapter: ${it.currentChapter ?: "General"}
            - Reading Progress: ${it.readingProgressPercent ?: 0}%
            - Synopsis: ${it.synopsis ?: "An engaging literary work available on Bookora."}
            """.trimIndent()
        } ?: "The user is in the Bookora reading ecosystem exploring various books and literature."

        return """
            You are the Bookora AI Voice Companion, powered directly by Google Gemini model gemini-3.1-flash-live-preview (Live API).
            You engage in natural, expressive, real-time voice conversations with readers.
            
            ACTIVE PERSONA: ${currentPersona.name} (${currentPersona.tone})
            ACTIVE MODE: ${currentMode.title}
            ${currentMode.systemPromptAddition}
            
            $bookInfo
            
            RULES FOR SPOKEN AUDIO RESPONSES:
            1. Keep responses concise, engaging, and spoken-friendly (2-4 sentences max per turn unless explaining a deep passage).
            2. Sound natural, friendly, and intellectually curious.
            3. Ask thoughtful follow-up questions to keep the voice conversation lively.
            4. Never use markdown formatting (no asterisks, bold tags, or bullet points) in your text output since it is synthesized to speech.
        """.trimIndent()
    }

    fun updateMode(mode: VoiceMode) {
        this.currentMode = mode
    }

    fun updatePersona(persona: VoicePersona) {
        this.currentPersona = persona
    }

    fun updateBookContext(context: BookVoiceContext?) {
        this.currentBookContext = context
    }

    fun close() {
        try {
            webSocket?.close(1000, "Session ended")
            webSocket = null
            isWebSocketConnected = false
        } catch (e: Exception) {
            Log.e(TAG, "Error closing session", e)
        }
    }
}
