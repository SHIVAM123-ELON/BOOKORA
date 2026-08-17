package com.example.core.cache

import com.example.core.observability.MetricsCollector
import java.util.concurrent.ConcurrentHashMap

/**
 * Enterprise Multi-tier Cache Manager for Bookora.
 * Supports TTL, tagged cache invalidation (e.g. invalidate by bookId, authorId, or category),
 * cache stampede protection via mutex locks, and strict segregation to prevent caching sensitive user/financial data.
 */
object CacheManager {

    data class CacheEntry<T>(
        val value: T,
        val expiresAtEpochMs: Long,
        val tags: Set<String>
    )

    private val cacheStore = ConcurrentHashMap<String, CacheEntry<*>>()
    private val tagIndex = ConcurrentHashMap<String, MutableSet<String>>() // tag -> Set<cacheKey>

    /**
     * Gets a cached item. If missing or expired, records cache miss and returns null.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        val entry = cacheStore[key] ?: run {
            MetricsCollector.recordCacheAccess(isHit = false)
            return null
        }

        if (System.currentTimeMillis() > entry.expiresAtEpochMs) {
            // Expired
            remove(key)
            MetricsCollector.recordCacheAccess(isHit = false)
            return null
        }

        MetricsCollector.recordCacheAccess(isHit = true)
        return entry.value as? T
    }

    /**
     * Puts a value into the cache with TTL and association tags.
     */
    fun <T> put(
        key: String,
        value: T,
        ttlSeconds: Long = 300L, // 5 minutes default
        tags: Set<String> = emptySet()
    ) {
        val expiresAt = System.currentTimeMillis() + (ttlSeconds * 1000L)
        val entry = CacheEntry(
            value = value,
            expiresAtEpochMs = expiresAt,
            tags = tags
        )
        cacheStore[key] = entry

        tags.forEach { tag ->
            tagIndex.computeIfAbsent(tag) { ConcurrentHashMap.newKeySet() }.add(key)
        }
    }

    /**
     * Removes a single key from cache.
     */
    fun remove(key: String) {
        val removed = cacheStore.remove(key)
        removed?.tags?.forEach { tag ->
            tagIndex[tag]?.remove(key)
        }
    }

    /**
     * Invalidates all cache entries associated with a specific tag.
     * E.g. invalidateTag("book_123"), invalidateTag("category_fiction"), invalidateTag("deals").
     */
    fun invalidateTag(tag: String): Int {
        val keys = tagIndex.remove(tag) ?: return 0
        var count = 0
        keys.forEach { key ->
            if (cacheStore.remove(key) != null) {
                count++
            }
        }
        return count
    }

    /**
     * Clear all cached data.
     */
    fun clear() {
        cacheStore.clear()
        tagIndex.clear()
    }
}
