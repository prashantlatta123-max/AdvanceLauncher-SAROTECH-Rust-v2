package com.sarotech.advancelauncher.system

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdvanceNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        refreshBadges()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshBadges()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshBadges()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _badgeCounts.value = emptyMap()
    }

    private fun refreshBadges() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        val counts = HashMap<String, Int>()
        for (sbn in active) {
            val n = sbn.notification ?: continue
            val isOngoing = (n.flags and Notification.FLAG_ONGOING_EVENT) != 0
            val isSummary = (n.flags and Notification.FLAG_GROUP_SUMMARY) != 0
            if (isOngoing || isSummary) continue
            val pkg = sbn.packageName ?: continue
            if (pkg == packageName) continue
            counts[pkg] = (counts[pkg] ?: 0) + 1
        }
        _badgeCounts.value = counts
    }

    companion object {
        private val _badgeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
        val badgeCounts: StateFlow<Map<String, Int>> = _badgeCounts.asStateFlow()

        fun isEnabled(context: Context): Boolean {
            val flat = runCatching {
                Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            }.getOrNull().orEmpty()
            return flat.contains(context.packageName)
        }

        fun openSettings(context: Context) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
        }
    }
}
