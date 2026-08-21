package com.example.data.local

import com.example.data.local.entity.review.*
import com.example.domain.model.review.ReviewModerationStatus
import com.example.domain.model.review.ReviewVerificationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReviewDatabaseSeeder(
    private val db: BookoraDatabase
) {
    suspend fun seedSampleReviewsIfEmpty() = withContext(Dispatchers.IO) {
        val existingReviews = db.reviewDao().getAllReviewsForModeration().first()
        if (existingReviews.isNotEmpty()) return@withContext

        // 1. Seed Verified Reader Entitlements & Reading Activities for default users
        val sampleVerifications = listOf(
            ReaderVerificationEntity(
                id = "rv-seed-001",
                userId = "u-reader-sarah-002",
                bookId = "b-clean-arch-001",
                isVerified = true,
                status = "VERIFIED",
                entitlementId = "ent-seed-001",
                entitlementSource = "PURCHASE",
                readingMinutes = 145.0,
                readingPercent = 88.5f,
                verificationReason = "Verified reader: Legitimate PURCHASE with 88.5% read.",
                verifiedAt = System.currentTimeMillis() - 86400000 * 12
            ),
            ReaderVerificationEntity(
                id = "rv-seed-002",
                userId = "u-reader-alex-003",
                bookId = "b-clean-arch-001",
                isVerified = true,
                status = "VERIFIED",
                entitlementId = "ent-seed-002",
                entitlementSource = "PURCHASE",
                readingMinutes = 210.0,
                readingPercent = 100.0f,
                verificationReason = "Verified reader: Legitimate PURCHASE with 100.0% read.",
                verifiedAt = System.currentTimeMillis() - 86400000 * 20
            ),
            ReaderVerificationEntity(
                id = "rv-seed-003",
                userId = "u-reader-marcus-004",
                bookId = "b-frontier-ai-002",
                isVerified = true,
                status = "VERIFIED",
                entitlementId = "ent-seed-003",
                entitlementSource = "SUBSCRIPTION",
                readingMinutes = 95.0,
                readingPercent = 64.0f,
                verificationReason = "Verified reader: Legitimate SUBSCRIPTION with 64.0% read.",
                verifiedAt = System.currentTimeMillis() - 86400000 * 5
            ),
            ReaderVerificationEntity(
                id = "rv-seed-004",
                userId = "u-reader-elena-005",
                bookId = "b-atomic-habits-003",
                isVerified = true,
                status = "VERIFIED",
                entitlementId = "ent-seed-004",
                entitlementSource = "PROMOTION",
                readingMinutes = 180.0,
                readingPercent = 95.0f,
                verificationReason = "Verified reader: Legitimate PROMOTION with 95.0% read.",
                verifiedAt = System.currentTimeMillis() - 86400000 * 15
            )
        )

        for (v in sampleVerifications) {
            db.readerVerificationDao().insertVerification(v)
        }

        // 2. Seed Realistic Verified Reviews
        val sampleReviews = listOf(
            BookReviewEntity(
                id = "rev-seed-001",
                userId = "u-reader-sarah-002",
                bookId = "b-clean-arch-001",
                userName = "Sarah Jenkins (Staff Architect)",
                userAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&auto=format&fit=crop&q=80",
                rating = 5,
                title = "Essential reading for serious backend & mobile architects",
                reviewText = "Uncle Bob articulates separation of concerns, dependency inversion, and boundaries better than almost anyone in software. Applying these principles directly reduced our module build times and untangled legacy codebases. Highly recommend for any senior engineer.",
                verificationStatus = ReviewVerificationStatus.VERIFIED_READER.name,
                moderationStatus = ReviewModerationStatus.PUBLISHED.name,
                helpfulCount = 42,
                reportCount = 0,
                isEdited = false,
                authorReply = "Thank you Sarah! Clean boundaries are the only way systems stand the test of time.",
                authorRepliedAt = System.currentTimeMillis() - 86400000 * 10,
                createdAt = System.currentTimeMillis() - 86400000 * 12
            ),
            BookReviewEntity(
                id = "rev-seed-002",
                userId = "u-reader-alex-003",
                bookId = "b-clean-arch-001",
                userName = "Alex Rivera",
                userAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&auto=format&fit=crop&q=80",
                rating = 5,
                title = "Crystal clear explanations of SOLID and Clean boundaries",
                reviewText = "Finished the entire book in two weeks. The chapters on component cohesion and component coupling are pure gold. The diagrammatic breakdowns make clean architecture intuitive.",
                verificationStatus = ReviewVerificationStatus.VERIFIED_READER.name,
                moderationStatus = ReviewModerationStatus.PUBLISHED.name,
                helpfulCount = 28,
                reportCount = 0,
                isEdited = false,
                createdAt = System.currentTimeMillis() - 86400000 * 18
            ),
            BookReviewEntity(
                id = "rev-seed-003",
                userId = "u-reader-dev-009",
                bookId = "b-clean-arch-001",
                userName = "David K.",
                userAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&auto=format&fit=crop&q=80",
                rating = 4,
                title = "Great concepts, slightly repetitive in middle chapters",
                reviewText = "The core thesis is brilliant and every engineer should understand ports & adapters. Some examples feel slightly dated to early 2000s Java, but the core paradigms translate 100% to modern Kotlin and Swift.",
                verificationStatus = ReviewVerificationStatus.UNVERIFIED_REVIEWER.name,
                moderationStatus = ReviewModerationStatus.PUBLISHED.name,
                helpfulCount = 14,
                reportCount = 0,
                isEdited = false,
                createdAt = System.currentTimeMillis() - 86400000 * 25
            ),
            BookReviewEntity(
                id = "rev-seed-004",
                userId = "u-reader-marcus-004",
                bookId = "b-frontier-ai-002",
                userName = "Marcus Chen (ML Researcher)",
                userAvatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=200&auto=format&fit=crop&q=80",
                rating = 5,
                title = "The best practical guide to reasoning models & agentic loops",
                reviewText = "DeepMind's breakdown of tool-use orchestration, chain-of-thought verification, and multi-agent coordination is unmatched. If you are building autonomous software in 2026, read this immediately.",
                verificationStatus = ReviewVerificationStatus.VERIFIED_READER.name,
                moderationStatus = ReviewModerationStatus.PUBLISHED.name,
                helpfulCount = 37,
                reportCount = 0,
                isEdited = false,
                createdAt = System.currentTimeMillis() - 86400000 * 5
            ),
            BookReviewEntity(
                id = "rev-seed-005",
                userId = "u-reader-elena-005",
                bookId = "b-atomic-habits-003",
                userName = "Elena Petrova",
                userAvatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200&auto=format&fit=crop&q=80",
                rating = 5,
                title = "Life-changing mental models for compounding daily consistency",
                reviewText = "The concept of identity-based habits completely shifted my perspective. James Clear makes complex behavioral psychology actionable and genuinely rewarding. A masterpiece.",
                verificationStatus = ReviewVerificationStatus.VERIFIED_READER.name,
                moderationStatus = ReviewModerationStatus.PUBLISHED.name,
                helpfulCount = 65,
                reportCount = 0,
                isEdited = false,
                createdAt = System.currentTimeMillis() - 86400000 * 14
            )
        )

        for (rev in sampleReviews) {
            db.reviewDao().insertReview(rev)
        }
    }

    companion object {
        fun seedDefaults(database: BookoraDatabase) {
            CoroutineScope(Dispatchers.IO).launch {
                val seeder = ReviewDatabaseSeeder(database)
                seeder.seedSampleReviewsIfEmpty()
            }
        }
    }
}
