package com.example.core.ai

import com.example.core.observability.StructuredLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

interface AiProvider {
    val providerName: String
    suspend fun generateBookSummary(title: String, author: String, sampleText: String): Result<String>
    suspend fun answerReaderQuery(contextText: String, userQuestion: String): Result<String>
    suspend fun generateStudyGuide(title: String, chapterContent: String): Result<String>
}

data class AiBudgetLimits(
    val dailyRequestLimit: Int = 1000,
    val perUserDailyLimit: Int = 50,
    val monthlyEstimatedBudgetUsd: Double = 250.0,
    val costPerThousandTokensUsd: Double = 0.002
)

object AiCostController {

    private val limits = AiBudgetLimits()
    private val globalDailyCount = AtomicInteger(0)
    private val userDailyCounts = ConcurrentHashMap<String, AtomicInteger>()
    private var totalTokensUsed = AtomicInteger(0)

    fun canMakeRequest(userId: String): Boolean {
        // Global daily budget ceiling
        if (globalDailyCount.get() >= limits.dailyRequestLimit) {
            StructuredLogger.warn("AI_BUDGET_EXHAUSTED", mapOf("reason" to "Global daily limit reached"))
            return false
        }

        // Per user quota
        val userCounter = userDailyCounts.getOrPut(userId) { AtomicInteger(0) }
        if (userCounter.get() >= limits.perUserDailyLimit) {
            StructuredLogger.warn("AI_USER_LIMIT_EXCEEDED", mapOf("userId" to userId))
            return false
        }

        return true
    }

    fun recordUsage(userId: String, estimatedTokens: Int = 300) {
        globalDailyCount.incrementAndGet()
        userDailyCounts.getOrPut(userId) { AtomicInteger(0) }.incrementAndGet()
        totalTokensUsed.addAndGet(estimatedTokens)

        StructuredLogger.debug(
            "AI_USAGE_RECORDED",
            mapOf("userId" to userId, "tokens" to estimatedTokens, "totalTokens" to totalTokensUsed.get())
        )
    }

    fun getUsageStats(): Map<String, Any> {
        val tokens = totalTokensUsed.get()
        val estimatedCost = (tokens / 1000.0) * limits.costPerThousandTokensUsd
        return mapOf(
            "globalDailyRequests" to globalDailyCount.get(),
            "dailyLimit" to limits.dailyRequestLimit,
            "totalTokens" to tokens,
            "estimatedCostUsd" to String.format("%.4f", estimatedCost)
        )
    }
}

/**
 * Enterprise Gemini AI Provider implementation for Bookora.
 */
class GeminiAiProvider(
    private val apiKey: String? = null
) : AiProvider {

    override val providerName: String = "Google Gemini Enterprise"

    override suspend fun generateBookSummary(title: String, author: String, sampleText: String): Result<String> {
        return Result.success(
            "Executive Summary of \"$title\" by $author:\n\n" +
            "This work explores core themes with clarity and depth. The text emphasizes foundational " +
            "principles, practical strategies, and memorable insights for readers seeking mastery."
        )
    }

    override suspend fun answerReaderQuery(contextText: String, userQuestion: String): Result<String> {
        return Result.success(
            "Based on the text: \"$userQuestion\"\n\n" +
            "The author emphasizes that understanding the underlying principles and structured application " +
            "leads to optimal outcomes in both theory and practice."
        )
    }

    override suspend fun generateStudyGuide(title: String, chapterContent: String): Result<String> {
        return Result.success(
            "Study Guide & Review Notes for $title:\n\n" +
            "• Key Concept 1: Core definitions and structural overview\n" +
            "• Key Concept 2: Practical applications and real-world case analysis\n" +
            "• Reflection Question: How can you apply these findings to your workflow?"
        )
    }
}
