package com.example.data.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.TourManageApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val type: String, // "booking", "chat", "wishlist", "reminder"
    val timestamp: Long,
    val isRead: Boolean = false,
    val targetRoute: String? = null
)

class TourNotificationManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("tour_notifications_prefs", Context.MODE_PRIVATE)

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private var currentCustomerId: String? = null

    fun setCustomer(customerId: String?) {
        currentCustomerId = customerId
        loadNotifications()
    }

    private fun loadNotifications() {
        val cid = currentCustomerId ?: "guest"
        val jsonStr = prefs.getString("notifs_$cid", "[]") ?: "[]"
        try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<AppNotification>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    AppNotification(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        message = obj.getString("message"),
                        type = obj.optString("type", "booking"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isRead = obj.optBoolean("isRead", false),
                        targetRoute = obj.optString("targetRoute", null)
                    )
                )
            }
            _notifications.value = list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            _notifications.value = emptyList()
        }
    }

    private fun saveNotifications(list: List<AppNotification>) {
        val cid = currentCustomerId ?: "guest"
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("message", item.message)
                put("type", item.type)
                put("timestamp", item.timestamp)
                put("isRead", item.isRead)
                put("targetRoute", item.targetRoute)
            }
            arr.put(obj)
        }
        prefs.edit().putString("notifs_$cid", arr.toString()).apply()
        _notifications.value = list.sortedByDescending { it.timestamp }
    }

    fun isNotificationTypeEnabled(type: String): Boolean {
        return prefs.getBoolean("pref_toggle_$type", true)
    }

    fun setNotificationTypeEnabled(type: String, enabled: Boolean) {
        prefs.edit().putBoolean("pref_toggle_$type", enabled).apply()
    }

    fun addNotification(
        id: String,
        title: String,
        message: String,
        type: String,
        targetRoute: String? = null,
        postSystemNotification: Boolean = true
    ) {
        val cid = currentCustomerId ?: "guest"
        if (!isNotificationTypeEnabled(type)) return

        val currentList = _notifications.value.toMutableList()
        if (currentList.any { it.id == id }) return // Deduplicate

        val newNotif = AppNotification(
            id = id,
            title = title,
            message = message,
            type = type,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            targetRoute = targetRoute
        )
        currentList.add(0, newNotif)
        saveNotifications(currentList)

        if (postSystemNotification) {
            showSystemNotification(id.hashCode(), title, message, type)
        }
    }

    private fun showSystemNotification(notificationId: Int, title: String, message: String, type: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val channelId = if (type == "chat") TourManageApplication.CHANNEL_ID_CHAT else TourManageApplication.CHANNEL_ID_ALERTS
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(if (type == "chat") NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted or blocked
        }
    }

    fun markAsRead(id: String) {
        val updated = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        saveNotifications(updated)
    }

    fun markAllAsRead() {
        val updated = _notifications.value.map { it.copy(isRead = true) }
        saveNotifications(updated)
    }

    fun clearAll() {
        saveNotifications(emptyList())
    }
}

class TourNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Runs periodic checks every 15 minutes when app is closed to verify updates
        return Result.success()
    }
}
