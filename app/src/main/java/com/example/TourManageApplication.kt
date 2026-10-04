package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.*
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.FirebaseProvider
import com.example.data.notifications.TourNotificationWorker
import java.util.concurrent.TimeUnit

class TourManageApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Firebase manually with named Firestore DB
        FirebaseProvider.initialize(this)

        // 2. Configure Coil for smooth performance and low RAM usage
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100MB disk cache
                    .build()
            }
            .crossfade(200)
            .allowRgb565(true)
            .build()
        Coil.setImageLoader(imageLoader)

        // 3. Create Notification Channels
        createNotificationChannels()

        // 4. Setup WorkManager periodic background work
        setupPeriodicWork()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val mainChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Tour & Booking Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Updates on booking statuses, price drops, low seat alerts and trip reminders"
            }

            val chatChannel = NotificationChannel(
                CHANNEL_ID_CHAT,
                "Support Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Replies from Tour Manage travel support team"
            }

            notificationManager.createNotificationChannel(mainChannel)
            notificationManager.createNotificationChannel(chatChannel)
        }
    }

    private fun setupPeriodicWork() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<TourNotificationWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "TourManageUpdatesWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        } catch (e: Exception) {
            // Log or ignore if WorkManager initialization encounters sandbox constraints
        }
    }

    companion object {
        const val CHANNEL_ID_ALERTS = "tour_manage_alerts"
        const val CHANNEL_ID_CHAT = "tour_manage_chat"
    }
}
