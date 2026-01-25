package com.example.fluentui.data

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationRepo {
    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts = _counts.asStateFlow()

    fun update(map: Map<String, Int>) {
        _counts.value = map
    }
}

class FluentNotificationService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        updateNotifications()
    }

    private fun updateNotifications() {
        try {
            val active = activeNotifications ?: return

            val map = active
                .filter { !it.isOngoing }
                .groupingBy { it.packageName }
                .eachCount()

            NotificationRepo.update(map)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}