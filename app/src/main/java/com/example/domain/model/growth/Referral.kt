package com.example.domain.model.growth

import java.util.UUID

enum class ReferralStatus {
    INVITED,
    REGISTERED,
    QUALIFIED,
    REWARDED,
    EXPIRED
}

enum class ReferralRewardType {
    WALLET_CREDIT,
    PERCENTAGE_DISCOUNT,
    FREE_BOOK_VOUCHER,
    FREE_MONTH_SUBSCRIPTION
}

data class ReferralReward(
    val type: ReferralRewardType,
    val value: Double,
    val currency: String = "INR",
    val description: String
)

data class Referral(
    val id: String = "ref_" + UUID.randomUUID().toString().take(12),
    val referrerUserId: String,
    val referredUserId: String? = null,
    val code: String,
    val status: ReferralStatus = ReferralStatus.INVITED,
    val reward: ReferralReward = ReferralReward(
        type = ReferralRewardType.WALLET_CREDIT,
        value = 100.0,
        currency = "INR",
        description = "₹100 Bookora Wallet Reading Credit"
    ),
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val qualifiedAt: Long? = null,
    val rewardedAt: Long? = null,
    val expiresAt: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000) // 30 days
)

data class Campaign(
    val id: String,
    val title: String,
    val authorId: String? = null,
    val code: String,
    val discountPercent: Double,
    val maxUses: Int,
    val currentUses: Int = 0,
    val startDate: Long,
    val endDate: Long,
    val isActive: Boolean = true
)
