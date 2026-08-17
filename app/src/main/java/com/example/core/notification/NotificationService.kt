package com.example.core.notification

import com.example.core.observability.StructuredLogger
import com.example.core.privacy.PrivacyController
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

enum class NotificationCategory {
    TRANSACTIONAL,
    MARKETING,
    READING_REMINDER,
    AUTHOR_ALERT,
    SUBSCRIPTION
}

data class InAppNotification(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val category: NotificationCategory,
    val deepLinkUri: String? = null,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

object NotificationService {

    private val userNotifications = ConcurrentHashMap<String, CopyOnWriteArrayList<InAppNotification>>()

    fun postNotification(
        userId: String,
        title: String,
        message: String,
        category: NotificationCategory,
        deepLinkUri: String? = null
    ): Boolean {
        // Respect privacy preferences for marketing
        val privacy = PrivacyController.getSettings(userId)
        if (category == NotificationCategory.MARKETING && !privacy.emailMarketingOptIn && !privacy.pushNotificationsOptIn) {
            StructuredLogger.debug("NOTIFICATION_DROPPED_OPT_OUT", mapOf("userId" to userId, "category" to category.name))
            return false
        }

        val notification = InAppNotification(
            id = "notif_" + System.currentTimeMillis(),
            userId = userId,
            title = title,
            message = message,
            category = category,
            deepLinkUri = deepLinkUri
        )

        val list = userNotifications.getOrPut(userId) { CopyOnWriteArrayList() }
        list.add(0, notification)

        // Cap list size
        while (list.size > 100) {
            list.removeAt(list.lastIndex)
        }

        StructuredLogger.info(
            "NOTIFICATION_DISPATCHED",
            mapOf("userId" to userId, "title" to title, "category" to category.name)
        )

        return true
    }

    fun getNotifications(userId: String): List<InAppNotification> {
        return userNotifications[userId]?.toList() ?: emptyList()
    }

    fun markAsRead(userId: String, notificationId: String) {
        userNotifications[userId]?.let { list ->
            val index = list.indexOfFirst { it.id == notificationId }
            if (index != -1) {
                list[index] = list[index].copy(isRead = true)
            }
        }
    }
}
