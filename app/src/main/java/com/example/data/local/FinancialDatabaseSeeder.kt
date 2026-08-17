package com.example.data.local

import com.example.data.local.entity.financial.*
import com.example.domain.model.financial.BillingPeriod
import com.example.domain.model.financial.CouponType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FinancialDatabaseSeeder {

    fun seedDefaults(database: BookoraDatabase) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Marketplace Settings
                val settings = database.marketplaceSettingsDao().getSettingsDirect()
                if (settings == null) {
                    database.marketplaceSettingsDao().insertSettings(
                        MarketplaceSettingsEntity(
                            id = "default_settings",
                            defaultPlatformCommissionRate = 0.20, // 20% platform commission
                            minimumPayoutMinor = 100000L,        // ₹1,000.00
                            payoutSchedule = "WEEKLY_ON_MONDAY",
                            defaultTaxRate = 0.00,
                            refundEntitlementPolicy = "REFUND_REVOKES_ACCESS",
                            currency = "INR",
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }

                // 2. Default Promotional Coupons
                val coupons = listOf(
                    CouponEntity(
                        id = "coup_welcome20",
                        code = "WELCOME20",
                        type = CouponType.PERCENTAGE.name,
                        value = 20L, // 20% off
                        maxDiscountMinor = 20000L, // max ₹200 discount
                        minimumOrderMinor = 19900L, // min ₹199 order
                        usageLimit = 5000,
                        perUserLimit = 1,
                        startsAt = 0L,
                        expiresAt = Long.MAX_VALUE,
                        status = "ACTIVE"
                    ),
                    CouponEntity(
                        id = "coup_bookora50",
                        code = "BOOKORA50",
                        type = CouponType.FIXED_AMOUNT.name,
                        value = 5000L, // ₹50 flat off
                        maxDiscountMinor = 5000L,
                        minimumOrderMinor = 29900L, // min ₹299 order
                        usageLimit = 2000,
                        perUserLimit = 2,
                        startsAt = 0L,
                        expiresAt = Long.MAX_VALUE,
                        status = "ACTIVE"
                    ),
                    CouponEntity(
                        id = "coup_festive30",
                        code = "FESTIVE30",
                        type = CouponType.PERCENTAGE.name,
                        value = 30L, // 30% off
                        maxDiscountMinor = 50000L, // max ₹500 discount
                        minimumOrderMinor = 49900L,
                        usageLimit = 1000,
                        perUserLimit = 1,
                        startsAt = 0L,
                        expiresAt = Long.MAX_VALUE,
                        status = "ACTIVE"
                    )
                )
                for (coupon in coupons) {
                    if (database.couponDao().getCouponByCode(coupon.code) == null) {
                        database.couponDao().insertCoupon(coupon)
                    }
                }

                // 3. Subscription Plans
                val plans = listOf(
                    SubscriptionPlanEntity(
                        id = "plan_monthly_unlimited",
                        name = "Bookora Unlimited Monthly",
                        description = "Unlimited reading access to 10,000+ bestsellers, AI reading assistant, offline downloads & audio previews.",
                        priceMinor = 29900L, // ₹299/mo
                        currency = "INR",
                        billingPeriod = BillingPeriod.MONTHLY.name,
                        status = "ACTIVE",
                        features = "Unlimited Library Access;;;Full Gemini Reading Assistant;;;Offline Encrypted Downloads;;;Author Direct Community;;;Zero Ads Guaranteed"
                    ),
                    SubscriptionPlanEntity(
                        id = "plan_annual_vip",
                        name = "Bookora VIP Annual",
                        description = "Save 30% with annual membership. Includes exclusive author masterclasses, priority early access releases & unlimited reading.",
                        priceMinor = 249900L, // ₹2,499/year (equals ₹208/mo)
                        currency = "INR",
                        billingPeriod = BillingPeriod.YEARLY.name,
                        status = "ACTIVE",
                        features = "All Monthly Unlimited Benefits;;;Exclusive Author Masterclasses;;;3 Free Premium Book Purchases Every Quarter;;;Priority Gemini Ultra AI Assistant;;;VIP Discord & Community Access"
                    )
                )
                for (plan in plans) {
                    database.subscriptionDao().insertPlan(plan)
                }

                // 4. Curated Book Bundles
                val bundles = listOf(
                    BundleEntity(
                        id = "bundle_system_architecture",
                        title = "Full-Stack System Architecture Bundle",
                        description = "Comprehensive bundle covering modern distributed systems, clean architecture, Kotlin and cloud databases.",
                        coverUrl = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=500&auto=format&fit=crop&q=80",
                        priceMinor = 79900L, // ₹799
                        originalPriceMinor = 129900L, // ₹1,299
                        currency = "INR",
                        bookIds = "b-arch-001,b-compose-002,b-gemini-003",
                        status = "ACTIVE"
                    ),
                    BundleEntity(
                        id = "bundle_mindset_habits",
                        title = "High-Performance Habits & Mindset Pack",
                        description = "Master your daily focus, atomic routines, and cognitive resilience with top psychological frameworks.",
                        coverUrl = "https://images.unsplash.com/photo-1499750310107-5fef28a66643?w=500&auto=format&fit=crop&q=80",
                        priceMinor = 49900L, // ₹499
                        originalPriceMinor = 89900L, // ₹899
                        currency = "INR",
                        bookIds = "b-mindset-004,b-focus-005",
                        status = "ACTIVE"
                    )
                )
                for (bundle in bundles) {
                    database.bundleDao().insertBundle(bundle)
                }

                // 5. Initial Author Wallet seed for Demo Author
                val demoAuthorWallet = database.authorWalletDao().getWalletByAuthorIdDirect("u-author-elena")
                if (demoAuthorWallet == null) {
                    database.authorWalletDao().insertWallet(
                        AuthorWalletEntity(
                            id = "wallet_elena_001",
                            authorId = "u-author-elena",
                            availableBalanceMinor = 1840000L, // ₹18,400.00
                            pendingBalanceMinor = 320000L,   // ₹3,200.00
                            lifetimeEarnedMinor = 4500000L,  // ₹45,000.00
                            lifetimePaidMinor = 2340000L,    // ₹23,400.00
                            currency = "INR",
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                // Defensive catch for non-fatal seeder issues
            }
        }
    }
}
