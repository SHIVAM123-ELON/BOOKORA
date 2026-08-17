package com.example.core.growth

import android.content.Context
import android.content.Intent

data class PublicShareMetadata(
    val title: String,
    val description: String,
    val shareUrl: String,
    val previewImageUrl: String? = null
)

/**
 * Enterprise Safe Share Manager for Bookora.
 * Generates public, privacy-safe share links and text payloads.
 * STRICTLY PREVENTS exposing signed e-book URLs, private reader tokens, or internal IDs.
 */
object SafeShareManager {

    private const val BASE_WEB_URL = "https://bookora.com"

    fun createBookShareMetadata(
        bookId: String,
        title: String,
        authorName: String,
        coverUrl: String
    ): PublicShareMetadata {
        return PublicShareMetadata(
            title = "$title by $authorName",
            description = "Check out \"$title\" by $authorName on Bookora — Discover, Read & Learn!",
            shareUrl = "$BASE_WEB_URL/books/$bookId",
            previewImageUrl = coverUrl
        )
    }

    fun createAuthorShareMetadata(
        authorId: String,
        penName: String,
        bio: String
    ): PublicShareMetadata {
        return PublicShareMetadata(
            title = "Author $penName on Bookora",
            description = "Read exclusive books by $penName on Bookora.",
            shareUrl = "$BASE_WEB_URL/authors/$authorId"
        )
    }

    fun createReferralShareMetadata(
        referralCode: String
    ): PublicShareMetadata {
        return PublicShareMetadata(
            title = "Join me on Bookora!",
            description = "Get ₹100 reading credit on Bookora with my invite code $referralCode: $BASE_WEB_URL/ref/$referralCode",
            shareUrl = "$BASE_WEB_URL/ref/$referralCode"
        )
    }

    fun launchShareSheet(context: Context, metadata: PublicShareMetadata) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, metadata.title)
            putExtra(Intent.EXTRA_TEXT, "${metadata.description}\n\n${metadata.shareUrl}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "Share via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
