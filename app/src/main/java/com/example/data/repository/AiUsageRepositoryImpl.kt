package com.example.data.repository

import com.example.data.local.BookoraDatabase
import com.example.data.local.entity.AiUsageEntity
import com.example.domain.model.AiFeature
import com.example.domain.repository.AiUsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AiUsageRepositoryImpl(
    private val database: BookoraDatabase
) : AiUsageRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun getTodayDateString(): String {
        return dateFormat.format(Date())
    }

    override suspend fun recordUsage(userId: String, feature: AiFeature, tokens: Int) {
        val today = getTodayDateString()
        val current = database.aiUsageDao().getUsageDirect(userId, feature.name, today)
        if (current != null) {
            val updated = current.copy(
                requestCount = current.requestCount + 1,
                tokenUsage = current.tokenUsage + tokens
            )
            database.aiUsageDao().insertOrUpdateUsage(updated)
        } else {
            val newUsage = AiUsageEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                feature = feature.name,
                requestCount = 1,
                tokenUsage = tokens,
                date = today
            )
            database.aiUsageDao().insertOrUpdateUsage(newUsage)
        }
    }

    override fun getTodayUsage(userId: String, feature: AiFeature): Flow<Int> {
        val today = getTodayDateString()
        return database.aiUsageDao().getUsage(userId, feature.name, today).map { entity ->
            entity?.requestCount ?: 0
        }
    }

    override fun canUseFeature(userId: String, feature: AiFeature, isPlusUser: Boolean): Flow<Boolean> {
        val today = getTodayDateString()
        val limit = if (isPlusUser) feature.dailyLimitPlus else feature.dailyLimitFree
        return database.aiUsageDao().getUsage(userId, feature.name, today).map { entity ->
            val count = entity?.requestCount ?: 0
            count < limit
        }
    }
}
