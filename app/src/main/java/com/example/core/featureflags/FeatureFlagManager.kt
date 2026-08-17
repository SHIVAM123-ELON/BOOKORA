package com.example.core.featureflags

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Enterprise Feature Flag Controller for Bookora.
 * Supports percentage-based rollouts, user targeting, and runtime overrides.
 */
enum class FeatureFlag(val defaultEnabled: Boolean, val rolloutPercentage: Int) {
    AI_READING_ASSISTANT(defaultEnabled = true, rolloutPercentage = 100),
    AI_EXECUTIVE_SUMMARY(defaultEnabled = true, rolloutPercentage = 100),
    SEMANTIC_SEARCH(defaultEnabled = true, rolloutPercentage = 100),
    SUBSCRIPTIONS(defaultEnabled = true, rolloutPercentage = 100),
    PROMO_BUNDLES(defaultEnabled = true, rolloutPercentage = 100),
    AUTHOR_PAYOUTS(defaultEnabled = true, rolloutPercentage = 100),
    DARK_MODE_EXPERIMENTAL(defaultEnabled = false, rolloutPercentage = 25),
    AUDIOBOOK_STREAMING(defaultEnabled = false, rolloutPercentage = 10)
}

object FeatureFlagManager {

    private val overrides = ConcurrentHashMap<FeatureFlag, Boolean>()

    /**
     * Determines whether a feature flag is active for a specific user ID.
     */
    fun isEnabled(flag: FeatureFlag, userId: String? = null): Boolean {
        // 1. Check explicit runtime override
        overrides[flag]?.let { return it }

        // 2. If 100% or 0%, return immediately
        if (flag.rolloutPercentage >= 100) return flag.defaultEnabled
        if (flag.rolloutPercentage <= 0) return false

        // 3. User-deterministic percentage hashing
        if (userId != null) {
            val userHash = abs(userId.hashCode()) % 100
            return userHash < flag.rolloutPercentage
        }

        return flag.defaultEnabled
    }

    /**
     * Overrides a feature flag dynamically at runtime (Admin/SuperAdmin control).
     */
    fun setOverride(flag: FeatureFlag, enabled: Boolean?) {
        if (enabled == null) {
            overrides.remove(flag)
        } else {
            overrides[flag] = enabled
        }
    }

    fun getAllFlags(userId: String? = null): Map<String, Boolean> {
        return FeatureFlag.values().associate { flag ->
            flag.name to isEnabled(flag, userId)
        }
    }

    fun reset() {
        overrides.clear()
    }
}
