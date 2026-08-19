package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AiConversationDao
import com.example.data.local.dao.AiMessageDao
import com.example.data.local.dao.AiUsageDao
import com.example.data.local.dao.AuthorDao
import com.example.data.local.dao.BookDao
import com.example.data.local.dao.BookSummaryDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.LibraryDao
import com.example.data.local.dao.ReadingProgressDao
import com.example.data.local.dao.RecentSearchDao
import com.example.data.local.dao.RecommendationEventDao
import com.example.data.local.dao.UserDao
import com.example.data.local.dao.WishlistDao
import com.example.data.local.dao.financial.*
import com.example.data.local.dao.publisher.*
import com.example.data.local.dao.review.*
import com.example.data.local.entity.AiConversationEntity
import com.example.data.local.entity.AiMessageEntity
import com.example.data.local.entity.AiUsageEntity
import com.example.data.local.entity.AuthorEntity
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.BookSummaryEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.LibraryEntity
import com.example.data.local.entity.ReadingProgressEntity
import com.example.data.local.entity.RecentSearchEntity
import com.example.data.local.entity.RecommendationEventEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.WishlistEntity
import com.example.data.local.entity.financial.*
import com.example.data.local.entity.publisher.*
import com.example.data.local.entity.review.*

@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        AuthorEntity::class,
        BookEntity::class,
        LibraryEntity::class,
        ReadingProgressEntity::class,
        WishlistEntity::class,
        RecentSearchEntity::class,
        AiConversationEntity::class,
        AiMessageEntity::class,
        AiUsageEntity::class,
        RecommendationEventEntity::class,
        BookSummaryEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        PaymentEntity::class,
        PaymentWebhookEventEntity::class,
        EntitlementEntity::class,
        CartItemEntity::class,
        CouponEntity::class,
        BundleEntity::class,
        RoyaltyLedgerEntity::class,
        AuthorWalletEntity::class,
        PayoutRequestEntity::class,
        AuthorVerificationEntity::class,
        RefundEntity::class,
        SubscriptionPlanEntity::class,
        SubscriptionEntity::class,
        FinancialAuditLogEntity::class,
        RiskEventEntity::class,
        MarketplaceSettingsEntity::class,
        BookSubmissionEntity::class,
        CreatorBalanceEntity::class,
        CreatorTransactionEntity::class,
        CreatorPayoutRequestEntity::class,
        CopyrightReportEntity::class,
        ReaderVerificationEntity::class,
        ReadingActivityEntity::class,
        ReadingSessionEntity::class,
        BookReviewEntity::class,
        ReviewHelpfulVoteEntity::class,
        ReviewReportEntity::class,
        ReviewAuditEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class BookoraDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun authorDao(): AuthorDao
    abstract fun bookDao(): BookDao
    abstract fun libraryDao(): LibraryDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun aiConversationDao(): AiConversationDao
    abstract fun aiMessageDao(): AiMessageDao
    abstract fun aiUsageDao(): AiUsageDao
    abstract fun recommendationEventDao(): RecommendationEventDao
    abstract fun bookSummaryDao(): BookSummaryDao

    abstract fun orderDao(): OrderDao
    abstract fun paymentDao(): PaymentDao
    abstract fun webhookEventDao(): WebhookEventDao
    abstract fun entitlementDao(): EntitlementDao
    abstract fun cartDao(): CartDao
    abstract fun couponDao(): CouponDao
    abstract fun bundleDao(): BundleDao
    abstract fun royaltyLedgerDao(): RoyaltyLedgerDao
    abstract fun authorWalletDao(): AuthorWalletDao
    abstract fun payoutRequestDao(): PayoutRequestDao
    abstract fun authorVerificationDao(): AuthorVerificationDao
    abstract fun refundDao(): RefundDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun financialAuditLogDao(): FinancialAuditLogDao
    abstract fun riskEventDao(): RiskEventDao
    abstract fun marketplaceSettingsDao(): MarketplaceSettingsDao

    // Phase 8 Open Publisher DAOs
    abstract fun bookSubmissionDao(): BookSubmissionDao
    abstract fun creatorBalanceDao(): CreatorBalanceDao
    abstract fun creatorTransactionDao(): CreatorTransactionDao
    abstract fun creatorPayoutRequestDao(): CreatorPayoutRequestDao
    abstract fun copyrightReportDao(): CopyrightReportDao

    // Phase 9 Verified Reader & Trusted Reviews DAOs
    abstract fun readerVerificationDao(): ReaderVerificationDao
    abstract fun readingActivityDao(): ReadingActivityDao
    abstract fun reviewDao(): ReviewDao
    abstract fun reviewHelpfulVoteDao(): ReviewHelpfulVoteDao
    abstract fun reviewReportDao(): ReviewReportDao
    abstract fun reviewAuditDao(): ReviewAuditDao

    companion object {
        @Volatile
        private var INSTANCE: BookoraDatabase? = null

        fun getInstance(context: Context): BookoraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BookoraDatabase::class.java,
                    "bookora_local_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
