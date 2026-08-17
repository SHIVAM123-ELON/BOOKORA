package com.example.core.worker

import com.example.core.observability.StructuredLogger
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.min
import kotlin.math.pow

/**
 * Enterprise Resilient Background Job Queue & Worker Subsystem for Bookora.
 * Features:
 * - Idempotency tracking (deduplicating tasks by key)
 * - Exponential backoff with jitter and max retry ceiling (preventing infinite loops)
 * - Dead Letter Queue (DLQ) for permanently failed tasks
 * - Job Types: Payment Reconciliation, Subscription Sync, Payout Processing, Notification,
 *   Email Delivery, Analytics Aggregation, Search Indexing, Expired Entitlements Cleanup.
 */
enum class JobType {
    PAYMENT_RECONCILIATION,
    SUBSCRIPTION_SYNC,
    PAYOUT_PROCESSING,
    NOTIFICATION_DELIVERY,
    EMAIL_DELIVERY,
    ANALYTICS_AGGREGATION,
    SEARCH_INDEXING,
    EXPIRED_ENTITLEMENT_CLEANUP
}

enum class JobStatus { PENDING, PROCESSING, COMPLETED, RETRYING, FAILED_DLQ }

data class BackgroundJob(
    val id: String,
    val type: JobType,
    val idempotencyKey: String,
    val payload: Map<String, String>,
    var status: JobStatus = JobStatus.PENDING,
    var retryCount: Int = 0,
    val maxRetries: Int = 3,
    var nextAttemptEpochMs: Long = System.currentTimeMillis(),
    var lastError: String? = null,
    val createdAt: String = Instant.now().toString(),
    var completedAt: String? = null
)

object BackgroundJobEngine {

    private val jobStore = ConcurrentHashMap<String, BackgroundJob>()
    private val idempotencyIndex = ConcurrentHashMap<String, String>() // idempotencyKey -> jobId
    private val deadLetterQueue = CopyOnWriteArrayList<BackgroundJob>()

    /**
     * Enqueues a new background job. If a job with the same idempotencyKey already exists,
     * returns the existing job to guarantee idempotency.
     */
    fun enqueue(
        type: JobType,
        idempotencyKey: String,
        payload: Map<String, String>,
        maxRetries: Int = 3
    ): BackgroundJob {
        val existingJobId = idempotencyIndex[idempotencyKey]
        if (existingJobId != null) {
            val existing = jobStore[existingJobId]
            if (existing != null) {
                StructuredLogger.info(
                    message = "Job duplicate suppressed via idempotency key: $idempotencyKey",
                    metadata = mapOf("jobId" to existing.id, "type" to type.name)
                )
                return existing
            }
        }

        val jobId = "job_" + System.currentTimeMillis() + "_" + (1000..9999).random()
        val job = BackgroundJob(
            id = jobId,
            type = type,
            idempotencyKey = idempotencyKey,
            payload = payload,
            maxRetries = maxRetries
        )

        jobStore[jobId] = job
        idempotencyIndex[idempotencyKey] = jobId

        StructuredLogger.info(
            message = "Enqueued background job: ${type.name} ($jobId)",
            metadata = mapOf("jobId" to jobId, "type" to type.name, "idempotencyKey" to idempotencyKey)
        )

        return job
    }

    /**
     * Executes a job with automatic exponential backoff retry and DLQ routing.
     */
    fun executeJob(jobId: String, runner: (BackgroundJob) -> Boolean): Boolean {
        val job = jobStore[jobId] ?: return false
        job.status = JobStatus.PROCESSING

        try {
            val success = runner(job)
            if (success) {
                job.status = JobStatus.COMPLETED
                job.completedAt = Instant.now().toString()
                StructuredLogger.info(
                    message = "Job completed successfully: ${job.type.name} (${job.id})",
                    metadata = mapOf("jobId" to job.id, "type" to job.type.name)
                )
                return true
            } else {
                handleJobFailure(job, "Job runner returned failure status")
                return false
            }
        } catch (e: Exception) {
            handleJobFailure(job, e.message ?: "Unknown worker exception")
            return false
        }
    }

    private fun handleJobFailure(job: BackgroundJob, errorMsg: String) {
        job.retryCount++
        job.lastError = errorMsg

        if (job.retryCount <= job.maxRetries) {
            job.status = JobStatus.RETRYING
            // Exponential backoff: base 2 sec * 2^(retry-1) (capped at 60s) + jitter
            val backoffMs = min(60_000L, (2000L * (2.0.pow(job.retryCount - 1))).toLong()) + (0..500).random()
            job.nextAttemptEpochMs = System.currentTimeMillis() + backoffMs

            StructuredLogger.warn(
                message = "Job failed, scheduled for retry ${job.retryCount}/${job.maxRetries} in ${backoffMs}ms: ${job.type.name} (${job.id})",
                errorCode = "JOB_RETRY_SCHEDULED",
                metadata = mapOf("jobId" to job.id, "retry" to job.retryCount, "error" to errorMsg)
            )
        } else {
            job.status = JobStatus.FAILED_DLQ
            deadLetterQueue.add(job)

            StructuredLogger.error(
                message = "Job exhausted all retries, moved to Dead Letter Queue (DLQ): ${job.type.name} (${job.id})",
                errorCode = "JOB_EXHAUSTED_DLQ",
                metadata = mapOf("jobId" to job.id, "type" to job.type.name, "error" to errorMsg)
            )
        }
    }

    fun getJob(jobId: String): BackgroundJob? = jobStore[jobId]
    fun getDeadLetterQueue(): List<BackgroundJob> = deadLetterQueue.toList()
    fun getPendingJobs(): List<BackgroundJob> = jobStore.values.filter { it.status == JobStatus.PENDING || it.status == JobStatus.RETRYING }

    fun clearAll() {
        jobStore.clear()
        idempotencyIndex.clear()
        deadLetterQueue.clear()
    }
}
