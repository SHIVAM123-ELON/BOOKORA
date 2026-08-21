package com.example.domain.model.voice

enum class VoiceSender {
    USER,
    GEMINI_LIVE,
    SYSTEM
}

enum class LiveConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

enum class VoiceMode(
    val title: String,
    val subtitle: String,
    val systemPromptAddition: String
) {
    BOOK_DISCUSS(
        title = "Deep-Dive & Discuss",
        subtitle = "Debate themes, plot twists, and character arcs",
        systemPromptAddition = "You are a literary analyst and companion. Discuss themes, symbolism, character motivations, and plot depth with the reader."
    ),
    READING_COACH(
        title = "Comprehension Coach",
        subtitle = "Test understanding, explain concepts, and summarize",
        systemPromptAddition = "You are a friendly reading coach. Help the user understand difficult passages, quiz them on key concepts, and provide concise chapter summaries."
    ),
    STORYTELLER(
        title = "Immersive Storyteller",
        subtitle = "Dramatic narration and character roleplay",
        systemPromptAddition = "You are a dramatic narrator and voice actor. Speak with expressiveness, emotion, and roleplay characters from the story when asked."
    ),
    DISCOVERY(
        title = "Book Recommender",
        subtitle = "Find your next great read tailored to your taste",
        systemPromptAddition = "You are a book curator. Give personalized, enthusiastic recommendations based on the reader's mood, favorite tropes, and reading history."
    )
}

data class VoicePersona(
    val id: String,
    val name: String,
    val voiceCode: String, // e.g. "Aoede", "Charon", "Fenrir", "Kore", "Puck"
    val description: String,
    val tone: String,
    val previewGreeting: String
) {
    companion object {
        val ALL = listOf(
            VoicePersona(
                id = "kore",
                name = "Kore",
                voiceCode = "Kore",
                description = "Warm, gentle, and encouraging literary guide",
                tone = "Gentle & Inspiring",
                previewGreeting = "Hello! I'm Kore, your Bookora voice companion. What book are we exploring today?"
            ),
            VoicePersona(
                id = "charon",
                name = "Charon",
                voiceCode = "Charon",
                description = "Deep, scholarly, and articulate literature professor",
                tone = "Scholarly & Insightful",
                previewGreeting = "Greetings. I am Charon. Let's delve into the deeper literary themes together."
            ),
            VoicePersona(
                id = "aoede",
                name = "Aoede",
                voiceCode = "Aoede",
                description = "Poetic, expressive, and thoughtful storyteller",
                tone = "Poetic & Expressive",
                previewGreeting = "Welcome. I am Aoede. Stories are windows to the soul—what would you like to discuss?"
            ),
            VoicePersona(
                id = "fenrir",
                name = "Fenrir",
                voiceCode = "Fenrir",
                description = "Energetic, dynamic, and passionately curious reader",
                tone = "Energetic & Bold",
                previewGreeting = "Hey there! Ready to break down epic plot twists? Let's jump right in!"
            ),
            VoicePersona(
                id = "puck",
                name = "Puck",
                voiceCode = "Puck",
                description = "Witty, playful, and quick with clever book trivia",
                tone = "Witty & Playful",
                previewGreeting = "Hi! Puck at your service. Tell me what you're reading—or looking for—and let's make it fun!"
            )
        )

        val DEFAULT = ALL[0] // Kore
    }
}

data class VoiceChatMessage(
    val id: String,
    val sender: VoiceSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val audioDurationMs: Long? = null,
    val isStreaming: Boolean = false
)

data class BookVoiceContext(
    val bookId: String,
    val title: String,
    val author: String,
    val coverUrl: String? = null,
    val currentChapter: String? = null,
    val readingProgressPercent: Int? = null,
    val synopsis: String? = null
)
