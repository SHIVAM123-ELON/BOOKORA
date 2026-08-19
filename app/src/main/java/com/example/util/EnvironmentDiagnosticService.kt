package com.example.util

import android.util.Log
import com.example.BuildConfig

/**
 * Diagnostic Service to verify runtime environment configuration,
 * BuildConfig field injection from .env/.env.example, and payment gateway mode.
 */
object EnvironmentDiagnosticService {

    private const val TAG = "BookoraEnvDiagnostic"

    /**
     * Reads ALLOW_MOCK_PAYMENTS from BuildConfig (populated by Secrets Gradle Plugin from .env / .env.example)
     * and returns the resolved boolean value.
     */
    fun isMockPaymentsAllowed(): Boolean {
        return try {
            // Check if BuildConfig has ALLOW_MOCK_PAYMENTS generated
            val field = BuildConfig::class.java.getField("ALLOW_MOCK_PAYMENTS")
            val rawValue = field.get(null)?.toString()?.trim() ?: "true"
            rawValue.equals("true", ignoreCase = true) || rawValue == "1"
        } catch (e: NoSuchFieldException) {
            // Fallback to BuildConfig.DEBUG if the custom field is not present
            Log.w(TAG, "ALLOW_MOCK_PAYMENTS field not present in BuildConfig, defaulting to DEBUG (${BuildConfig.DEBUG})")
            BuildConfig.DEBUG
        } catch (e: Exception) {
            Log.e(TAG, "Error checking ALLOW_MOCK_PAYMENTS: ${e.message}", e)
            BuildConfig.DEBUG
        }
    }

    /**
     * Prints complete startup diagnostics to Logcat for auditing.
     */
    fun logStartupDiagnostics() {
        val mockPaymentsAllowed = isMockPaymentsAllowed()
        val buildType = if (BuildConfig.DEBUG) "DEBUG" else "RELEASE"
        val appId = BuildConfig.APPLICATION_ID

        Log.i(TAG, "====================================================================")
        Log.i(TAG, "🚀 BOOKORA RUNTIME ENVIRONMENT STARTUP DIAGNOSTICS")
        Log.i(TAG, "====================================================================")
        Log.i(TAG, "📦 Application ID: $appId")
        Log.i(TAG, "⚙️  Build Type: $buildType")
        Log.i(TAG, "💳 ALLOW_MOCK_PAYMENTS (BuildConfig / .env): $mockPaymentsAllowed")
        if (mockPaymentsAllowed) {
            Log.i(TAG, "🧪 Payment Mode: SANDBOX / DEVELOPMENT SIMULATOR ENABLED")
        } else {
            Log.i(TAG, "🔒 Payment Mode: PRODUCTION GATEWAY ENFORCED (Sandbox disabled)")
        }
        Log.i(TAG, "====================================================================")
    }
}
