package com.example.core.config

/**
 * Enterprise Production Configuration & Fail-Fast Validator for Bookora.
 * Strictly verifies environment configurations before application startup.
 * Prevents running mock payment processors, unauthenticated debug AI proxies,
 * or insecure database credentials in production environments.
 */
enum class EnvironmentType {
    DEVELOPMENT,
    STAGING,
    PRODUCTION
}

data class AppConfig(
    val environment: EnvironmentType,
    val databaseUrl: String,
    val redisUrl: String,
    val objectStorageBucket: String,
    val jwtSecret: String,
    val geminiApiKey: String?,
    val stripeSecretKey: String?,
    val allowMockPayments: Boolean,
    val allowMockAi: Boolean,
    val apiPort: Int = 8080
)

object ProductionConfig {

    data class ValidationReport(
        val isValid: Boolean,
        val environment: EnvironmentType,
        val criticalErrors: List<String>,
        val warnings: List<String>
    )

    /**
     * Strictly validates the active configuration.
     * Enforces FAIL-FAST behavior for production environments.
     */
    fun validate(config: AppConfig): ValidationReport {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (config.environment == EnvironmentType.PRODUCTION) {
            // 1. JWT Secret strength
            if (config.jwtSecret.isBlank() || config.jwtSecret.length < 32 || config.jwtSecret.contains("default") || config.jwtSecret.contains("secret")) {
                errors.add("FAIL FAST: Production JWT secret must be at least 32 cryptographically secure characters and not contain placeholder strings.")
            }

            // 2. Mock payment prohibition
            if (config.allowMockPayments) {
                errors.add("FAIL FAST: Mock payment gateway is strictly prohibited in PRODUCTION mode.")
            }
            if (config.stripeSecretKey.isNullOrBlank() || config.stripeSecretKey.startsWith("sk_test_")) {
                errors.add("FAIL FAST: Production mode requires a live, valid payment gateway secret key.")
            }

            // 3. Database URL validation
            if (config.databaseUrl.isBlank() || config.databaseUrl.contains("localhost") || config.databaseUrl.contains("127.0.0.1")) {
                errors.add("FAIL FAST: Production database URL must point to a secure clustered database host, not localhost.")
            }

            // 4. Redis URL validation
            if (config.redisUrl.isBlank()) {
                errors.add("FAIL FAST: Production requires a dedicated Redis cluster URL for rate limiting and cache distribution.")
            }

            // 5. Object Storage
            if (config.objectStorageBucket.isBlank() || config.objectStorageBucket.contains("dev-bucket")) {
                errors.add("FAIL FAST: Production object storage bucket must be configured for protected e-book vaults.")
            }

            // 6. Gemini AI
            if (config.geminiApiKey.isNullOrBlank() || config.geminiApiKey.contains("MY_GEMINI_API_KEY")) {
                warnings.add("Gemini API key is missing or using placeholder; AI assistant features will run in degraded mode.")
            }
            if (config.allowMockAi) {
                warnings.add("Mock AI mode is enabled in production configuration.")
            }
        } else {
            // Development / Staging checks
            if (config.jwtSecret.isBlank()) {
                warnings.add("JWT secret is empty; defaulting to development token signer.")
            }
        }

        return ValidationReport(
            isValid = errors.isEmpty(),
            environment = config.environment,
            criticalErrors = errors,
            warnings = warnings
        )
    }
}
