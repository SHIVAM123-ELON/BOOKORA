package com.example

import com.example.core.cache.CacheManager
import com.example.core.featureflags.FeatureFlag
import com.example.core.featureflags.FeatureFlagManager
import com.example.core.observability.HealthCheckManager
import com.example.core.observability.MetricsCollector
import com.example.core.observability.StructuredLogger
import com.example.core.security.FileUploadValidator
import com.example.core.security.Permission
import com.example.core.security.PrivacyManager
import com.example.core.security.RateLimitTier
import com.example.core.security.RateLimiter
import com.example.core.security.RbacController
import com.example.core.security.SecurityManager
import com.example.core.security.SignedUrlManager
import com.example.core.security.UserRole
import com.example.core.worker.BackgroundJobEngine
import com.example.core.worker.JobStatus
import com.example.core.worker.JobType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SecurityAndHardeningTest {

    @Before
    fun setUp() {
        RateLimiter.resetLimits()
        CacheManager.clear()
        BackgroundJobEngine.clearAll()
        FeatureFlagManager.reset()
        MetricsCollector.reset()
    }

    @Test
    fun testPasswordPolicyValidation() {
        val weakResult = SecurityManager.validatePassword("12345")
        assertFalse(weakResult.isValid)
        assertTrue(weakResult.errors.isNotEmpty())

        val strongResult = SecurityManager.validatePassword("P@ssw0rdSecure!2026")
        assertTrue(strongResult.isValid)
        assertTrue(strongResult.errors.isEmpty())
    }

    @Test
    fun testPasswordHashingAndVerification() {
        val password = "MySecretProductionPassword#99"
        val hash = SecurityManager.hashPassword(password)
        assertTrue(hash.contains(":"))

        val isMatch = SecurityManager.verifyPassword(password, hash)
        assertTrue(isMatch)

        val isWrong = SecurityManager.verifyPassword("WrongPassword", hash)
        assertFalse(isWrong)
    }

    @Test
    fun testAccountLockoutAfterFailedAttempts() {
        val account = "user_attacker@test.com"
        assertFalse(SecurityManager.isAccountLocked(account))

        // 4 failed attempts should not lock yet
        for (i in 1..4) {
            SecurityManager.recordLoginAttempt(account, isSuccess = false)
            assertFalse(SecurityManager.isAccountLocked(account))
        }

        // 5th failed attempt triggers lockout
        SecurityManager.recordLoginAttempt(account, isSuccess = false)
        assertTrue(SecurityManager.isAccountLocked(account))
        assertTrue(SecurityManager.getRemainingLockoutSeconds(account) > 0)
    }

    @Test
    fun testRbacPermissionMatrix() {
        // Reader has book:read, but NOT book:approve or payout:request
        assertTrue(RbacController.hasPermission(UserRole.READER, Permission.BOOK_READ))
        assertFalse(RbacController.hasPermission(UserRole.READER, Permission.BOOK_APPROVE))
        assertFalse(RbacController.hasPermission(UserRole.READER, Permission.PAYOUT_REQUEST))

        // Author has book:create and payout:request, but NOT refund:approve
        assertTrue(RbacController.hasPermission(UserRole.AUTHOR, Permission.BOOK_CREATE))
        assertTrue(RbacController.hasPermission(UserRole.AUTHOR, Permission.PAYOUT_REQUEST))
        assertFalse(RbacController.hasPermission(UserRole.AUTHOR, Permission.REFUND_APPROVE))

        // Admin has book:approve, refund:approve, payout:approve, but NOT commission:change
        assertTrue(RbacController.hasPermission(UserRole.ADMIN, Permission.BOOK_APPROVE))
        assertTrue(RbacController.hasPermission(UserRole.ADMIN, Permission.REFUND_APPROVE))
        assertTrue(RbacController.hasPermission(UserRole.ADMIN, Permission.PAYOUT_APPROVE))
        assertFalse(RbacController.hasPermission(UserRole.ADMIN, Permission.COMMISSION_CHANGE))

        // Super Admin has all permissions
        assertTrue(RbacController.hasPermission(UserRole.SUPER_ADMIN, Permission.COMMISSION_CHANGE))
        assertTrue(RbacController.hasPermission(UserRole.SUPER_ADMIN, Permission.SYSTEM_CONFIGURE))
    }

    @Test
    fun testSignedUrlGenerationAndValidation() {
        val bookId = "book_quantum_computing"
        val userId = "user_alice_456"

        val signedUrl = SignedUrlManager.generateSignedDownloadUrl(bookId, userId, "epub", durationSeconds = 600)
        assertNotNull(signedUrl.signature)
        assertNotNull(signedUrl.token)
        assertTrue(signedUrl.url.contains("cdn.bookora.com/vault"))

        // Validate legitimate URL
        val validation = SignedUrlManager.validateSignedUrl(signedUrl.token, signedUrl.signature, signedUrl.expiresAtEpochMs)
        assertTrue(validation.isValid)
        assertEquals(bookId, validation.bookId)
        assertEquals(userId, validation.userId)

        // Validate tampering detection (tampered signature)
        val tamperedValidation = SignedUrlManager.validateSignedUrl(signedUrl.token, "tampered_fake_signature", signedUrl.expiresAtEpochMs)
        assertFalse(tamperedValidation.isValid)
    }

    @Test
    fun testFileUploadValidatorMagicBytes() {
        val pdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x37) // %PDF-1.7
        val validPdf = FileUploadValidator.validateUpload("manuscript.pdf", 1024 * 500, pdfBytes, "ebook")
        assertTrue(validPdf.isAllowed)

        val maliciousFakePdf = byteArrayOf(0x4D.toByte(), 0x5A.toByte(), 0x90.toByte(), 0x00.toByte()) // Windows EXE PE header disguised as .pdf
        val invalidResult = FileUploadValidator.validateUpload("malware.pdf", 1024 * 500, maliciousFakePdf, "ebook")
        assertFalse(invalidResult.isAllowed)
    }

    @Test
    fun testRateLimiterTokenConsumptionAndRejection() {
        val clientIp = "192.168.1.100"

        // Auth tier allows 5 requests
        for (i in 1..5) {
            val decision = RateLimiter.tryAcquire(clientIp, RateLimitTier.AUTH)
            assertTrue("Request $i should be allowed", decision.isAllowed)
        }

        // 6th request is throttled (HTTP 429)
        val throttled = RateLimiter.tryAcquire(clientIp, RateLimitTier.AUTH)
        assertFalse(throttled.isAllowed)
        assertTrue(throttled.retryAfterSeconds > 0)
    }

    @Test
    fun testPrivacyManagerAccountAnonymization() {
        val userId = "user_real_john_doe_123"
        val result = PrivacyManager.processAccountDeletion(userId)

        assertTrue(result.isSuccess)
        assertTrue(result.personalDataErased)
        assertTrue(result.financialRecordsPreservedForLegalCompliance)
        assertTrue(result.pseudonymizedId.startsWith("anon_"))
    }

    @Test
    fun testBackgroundJobEngineIdempotencyAndExecution() {
        val idempotencyKey = "idem_payout_author_789"
        val payload = mapOf("authorId" to "auth_123", "amountCents" to "15000")

        // First enqueue
        val job1 = BackgroundJobEngine.enqueue(JobType.PAYOUT_PROCESSING, idempotencyKey, payload)
        assertNotNull(job1)

        // Duplicate enqueue returns same job
        val job2 = BackgroundJobEngine.enqueue(JobType.PAYOUT_PROCESSING, idempotencyKey, payload)
        assertEquals(job1.id, job2.id)

        // Execute job
        var executed = false
        val success = BackgroundJobEngine.executeJob(job1.id) {
            executed = true
            true
        }

        assertTrue(success)
        assertTrue(executed)
        assertEquals(JobStatus.COMPLETED, job1.status)
    }

    @Test
    fun testCacheManagerTagInvalidation() {
        CacheManager.put("book_item_1", "Book 1 Details", ttlSeconds = 60, tags = setOf("books", "category_fiction"))
        CacheManager.put("book_item_2", "Book 2 Details", ttlSeconds = 60, tags = setOf("books", "category_science"))
        CacheManager.put("user_profile", "User Alice", ttlSeconds = 60, tags = setOf("users"))

        assertEquals("Book 1 Details", CacheManager.get<String>("book_item_1"))
        assertEquals("Book 2 Details", CacheManager.get<String>("book_item_2"))

        // Invalidate fiction category
        val invalidated = CacheManager.invalidateTag("category_fiction")
        assertEquals(1, invalidated)
        assertEquals(null, CacheManager.get<String>("book_item_1"))
        assertEquals("Book 2 Details", CacheManager.get<String>("book_item_2"))
        assertEquals("User Alice", CacheManager.get<String>("user_profile"))
    }

    @Test
    fun testHealthAndReadinessChecks() {
        val liveness = HealthCheckManager.checkLiveness()
        assertEquals(HealthCheckManager.HealthStatus.UP, liveness.status)

        val readiness = HealthCheckManager.checkReadiness()
        assertEquals(HealthCheckManager.HealthStatus.UP, readiness.status)
        assertTrue(readiness.checks.containsKey("database"))
        assertTrue(readiness.checks.containsKey("redis"))
        assertTrue(readiness.checks.containsKey("storage"))
    }

    @Test
    fun testStructuredLoggerRedaction() {
        val logEntry = StructuredLogger.info(
            message = "Authorization header: Bearer eyJhbGciOiJIUzI1NiJ9.testToken and apiKey=secret_12345678",
            metadata = mapOf("password" to "SuperSecretPass!", "userId" to "usr_99")
        )

        val json = logEntry.toJsonString()
        assertFalse(json.contains("SuperSecretPass!"))
        assertTrue(json.contains("[REDACTED]"))
        assertFalse(json.contains("secret_12345678"))
    }
}
