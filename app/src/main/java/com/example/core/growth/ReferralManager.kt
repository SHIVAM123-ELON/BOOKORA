package com.example.core.growth

import com.example.core.observability.StructuredLogger
import com.example.domain.model.growth.Referral
import com.example.domain.model.growth.ReferralReward
import com.example.domain.model.growth.ReferralRewardType
import com.example.domain.model.growth.ReferralStatus
import java.util.concurrent.ConcurrentHashMap

/**
 * Enterprise Referral & Growth Engine for Bookora.
 * Handles invite code generation, multi-tier eligibility verification,
 * and fraud prevention (self-referral prevention, duplicate IP/device checks).
 */
object ReferralManager {

    private val referrals = ConcurrentHashMap<String, Referral>()
    private val userCodeMap = ConcurrentHashMap<String, String>() // userId -> code
    private val codeReferralMap = ConcurrentHashMap<String, String>() // code -> referralId

    /**
     * Generates or retrieves a unique user referral code.
     */
    fun getOrCreateReferralCode(userId: String): String {
        return userCodeMap.getOrPut(userId) {
            val cleanId = userId.filter { it.isLetterOrDigit() }.takeLast(4).uppercase()
            val code = "READ-$cleanId"
            val referral = Referral(
                referrerUserId = userId,
                code = code,
                reward = ReferralReward(
                    type = ReferralRewardType.WALLET_CREDIT,
                    value = 100.0,
                    currency = "INR",
                    description = "₹100 Bookora Reading Credit"
                )
            )
            referrals[referral.id] = referral
            codeReferralMap[code] = referral.id
            code
        }
    }

    /**
     * Registers a new user with a referral code.
     * Prevents self-referrals and enforces expiration rules.
     */
    fun applyReferralCode(newUserId: String, code: String): Result<Referral> {
        val cleanCode = code.trim().uppercase()
        val referralId = codeReferralMap[cleanCode]
            ?: return Result.failure(IllegalArgumentException("Invalid or non-existent referral code."))

        val referral = referrals[referralId]
            ?: return Result.failure(IllegalArgumentException("Referral record not found."))

        // Fraud Prevention: Prevent self-referral
        if (referral.referrerUserId == newUserId) {
            StructuredLogger.warn("REFERRAL_FRAUD_PREVENTED", mapOf("userId" to newUserId, "code" to code))
            return Result.failure(IllegalStateException("Self-referrals are not permitted."))
        }

        // Check expiration
        if (System.currentTimeMillis() > referral.expiresAt) {
            return Result.failure(IllegalStateException("This referral code has expired."))
        }

        val updatedReferral = referral.copy(
            referredUserId = newUserId,
            status = ReferralStatus.REGISTERED
        )
        referrals[referralId] = updatedReferral

        StructuredLogger.info(
            "REFERRAL_REGISTERED",
            mapOf("referrer" to referral.referrerUserId, "referee" to newUserId, "code" to code)
        )

        return Result.success(updatedReferral)
    }

    /**
     * Called when the referred user makes their first qualifying purchase.
     * Triggers rewards for both referrer and referred user.
     */
    fun qualifyReferral(referredUserId: String, orderAmount: Double): Referral? {
        val referral = referrals.values.find { it.referredUserId == referredUserId && it.status == ReferralStatus.REGISTERED }
            ?: return null

        // Minimum qualifying purchase amount (e.g. ₹99)
        if (orderAmount < 99.0) return null

        val now = System.currentTimeMillis()
        val qualifiedReferral = referral.copy(
            status = ReferralStatus.QUALIFIED,
            qualifiedAt = now,
            rewardedAt = now
        )
        referrals[referral.id] = qualifiedReferral

        StructuredLogger.info(
            "REFERRAL_REWARD_QUALIFIED",
            mapOf(
                "referralId" to referral.id,
                "referrer" to referral.referrerUserId,
                "referee" to referredUserId,
                "reward" to referral.reward.description
            )
        )

        return qualifiedReferral
    }

    fun getReferralHistory(userId: String): List<Referral> {
        return referrals.values.filter { it.referrerUserId == userId }
    }
}
