package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object TourNotificationHelper {
    const val CHANNEL_ID_BOOKINGS = "tour_channel_bookings"
    const val CHANNEL_ID_CHAT = "tour_channel_chat"
    const val CHANNEL_ID_ALERTS = "tour_channel_alerts"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val bookingsChannel = NotificationChannel(
                CHANNEL_ID_BOOKINGS,
                "Tour Bookings & Trips",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Updates on your booking confirmations, holds, and travel reminders"
            }

            val chatChannel = NotificationChannel(
                CHANNEL_ID_CHAT,
                "Support & Chat",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Replies from Tour Manage travel specialists and AI assistant"
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Price & Seat Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts on saved wishlist packages and price drops"
            }

            notificationManager.createNotificationChannels(listOf(bookingsChannel, chatChannel, alertsChannel))
        }
    }

    fun postNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        message: String
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
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
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted on Android 13+
        } catch (e: Exception) {
            // Ignore notification error
        }
    }
}
