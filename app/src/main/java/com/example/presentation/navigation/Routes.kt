package com.example.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    
    // Main App Shell Tabs
    object Home : Screen("home")
    object Explore : Screen("explore")
    object Library : Screen("library")
    object Wishlist : Screen("wishlist")
    object Profile : Screen("profile")
    
    // Feature screens
    object BookDetails : Screen("book_details/{bookId}") {
        fun createRoute(bookId: String) = "book_details/$bookId"
    }
    object Reader : Screen("reader/{bookId}") {
        fun createRoute(bookId: String) = "reader/$bookId"
    }
    object AuthorStudio : Screen("author_studio")
    object AdminControl : Screen("admin_control")

    // Financial & Monetization screens
    object Cart : Screen("cart")
    object Checkout : Screen("checkout?bookId={bookId}&couponCode={couponCode}") {
        fun createRoute(bookId: String = "", couponCode: String = "") = "checkout?bookId=$bookId&couponCode=$couponCode"
    }
    object OrderHistory : Screen("order_history")
    object Subscriptions : Screen("subscriptions")
    object AuthorFinancial : Screen("author_financial")
    object AdminFinancial : Screen("admin_financial")

    // Phase 8 Open Publisher & Creator Reward screens
    object UploadBook : Screen("upload_book")
    object CreatorEarnings : Screen("creator_earnings")
    object MyUploads : Screen("my_uploads")
    object AdminModeration : Screen("admin_moderation")

    // Phase 9 Verified Reader & Trusted Reviews screens
    object AdminReviewModeration : Screen("admin_review_moderation")

    // Live Gemini 3.1 Flash Voice Conversations
    object VoiceConversation : Screen("voice_conversation?bookId={bookId}") {
        fun createRoute(bookId: String = "") = "voice_conversation?bookId=$bookId"
    }

    // Phase 11: Barcode & QR Physical Book Scanner
    object BookScanner : Screen("book_scanner")
}

data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
)
