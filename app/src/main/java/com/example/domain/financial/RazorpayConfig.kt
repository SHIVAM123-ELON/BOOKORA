package com.example.domain.financial

import android.util.Log
import com.example.BuildConfig

/**
 * Server Configuration and Keys for Razorpay Payment Gateway.
 * In a production architecture, the Key Secret resides exclusively on the backend server.
 */
object RazorpayConfig {

    private const val TAG = "RazorpayConfig"

    // Default Test Keys provided for Sandbox / Test Mode
    const val DEFAULT_TEST_KEY_ID = "rzp_test_TSGcMCkfMrL3Yg"
    const val DEFAULT_TEST_KEY_SECRET = "xxaCXvxhFre61PeDpe82Dh1T"

    // Official Razorpay.me Merchant Profile & Direct Payment Page
    const val RAZORPAY_ME_PAGE_URL = "https://razorpay.me/@shivammaurya3643"
    const val RAZORPAY_ME_HANDLE = "@shivammaurya3643"
    const val RAZORPAY_ME_UPI_VPA = "shivammaurya3643@okhdfcbank"

    /**
     * Builds a direct Razorpay.me URL with optional amount and note.
     */
    fun getRazorpayMePaymentUrl(amountRupees: Double? = null, note: String? = null): String {
        val baseUrl = RAZORPAY_ME_PAGE_URL
        val params = mutableListOf<String>()
        if (amountRupees != null && amountRupees > 0) {
            params.add("amount=${amountRupees.toLong()}")
        }
        if (!note.isNullOrBlank()) {
            params.add("notes=${java.net.URLEncoder.encode(note, "UTF-8")}")
        }
        return if (params.isNotEmpty()) "$baseUrl?${params.joinToString("&")}" else baseUrl
    }

    /**
     * Resolves the Razorpay Key ID from BuildConfig (injected via .env by Secrets plugin)
     * or falls back to test key ID.
     */
    fun getKeyId(): String {
        return try {
            val field = BuildConfig::class.java.getField("RAZORPAY_KEY_ID")
            val value = field.get(null)?.toString()?.trim()
            if (!value.isNullOrBlank() && value != "rzp_test_placeholder_key") {
                value
            } else {
                DEFAULT_TEST_KEY_ID
            }
        } catch (e: Exception) {
            DEFAULT_TEST_KEY_ID
        }
    }

    /**
     * Resolves the Razorpay Key Secret for server-side verification.
     */
    fun getKeySecret(): String {
        return try {
            val field = BuildConfig::class.java.getField("RAZORPAY_KEY_SECRET")
            val value = field.get(null)?.toString()?.trim()
            if (!value.isNullOrBlank() && value != "rzp_secret_placeholder_key") {
                value
            } else {
                DEFAULT_TEST_KEY_SECRET
            }
        } catch (e: Exception) {
            DEFAULT_TEST_KEY_SECRET
        }
    }
}
