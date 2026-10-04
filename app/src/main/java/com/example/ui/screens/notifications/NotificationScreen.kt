package com.example.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.NotificationItem
import com.example.data.models.formatTimestampDateTime
import com.example.data.repository.TourRepository
import com.example.ui.components.EmptyState
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    repository: TourRepository,
    onNavigateBack: () -> Unit,
    onOpenTarget: (targetType: String, targetId: String) -> Unit
) {
    val notifications by repository.notifications.collectAsState()

    // Group by Today vs Earlier
    val now = System.currentTimeMillis()
    val todayStart = remember(now) {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.timeInMillis
    }

    val todayItems = remember(notifications, todayStart) {
        notifications.filter { it.createdAt >= todayStart }
    }
    val earlierItems = remember(notifications, todayStart) {
        notifications.filter { it.createdAt < todayStart }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Notifications", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (notifications.isNotEmpty()) {
                        TextButton(
                            onClick = { repository.markAllNotificationsAsRead() },
                            modifier = Modifier.testTag("mark_all_read_button")
                        ) {
                            Text("Mark read", fontSize = 12.sp)
                        }
                        IconButton(
                            onClick = { repository.clearNotifications() },
                            modifier = Modifier.testTag("clear_notifications_button")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear all", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                EmptyState(
                    icon = Icons.Outlined.NotificationsNone,
                    title = "No notifications yet",
                    subtitle = "You will receive updates here for booking confirmations, trip countdowns, price drops, and support replies."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (todayItems.isNotEmpty()) {
                    item {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(todayItems) { item ->
                        NotificationCard(
                            item = item,
                            onClick = {
                                repository.markNotificationAsRead(item.id)
                                onOpenTarget(item.targetType, item.targetId)
                            }
                        )
                    }
                }

                if (earlierItems.isNotEmpty()) {
                    item {
                        Text(
                            text = "Earlier",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(earlierItems) { item ->
                        NotificationCard(
                            item = item,
                            onClick = {
                                repository.markNotificationAsRead(item.id)
                                onOpenTarget(item.targetType, item.targetId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    item: NotificationItem,
    onClick: () -> Unit
) {
    val (icon, iconTint, iconBg) = when (item.type) {
        "booking" -> Triple(Icons.Default.Luggage, TourNavy, TourNavyLight)
        "chat" -> Triple(Icons.Default.Chat, StatusGreen, Color(0xFFDCFCE7))
        "wishlist_price" -> Triple(Icons.Default.TrendingDown, TourGoldDark, TourGoldLight)
        "wishlist_seats" -> Triple(Icons.Default.AirlineSeatReclineNormal, StatusOrange, Color(0xFFFFEDD5))
        "trip_reminder" -> Triple(Icons.Default.Event, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
        else -> Triple(Icons.Default.Notifications, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
    }

    val timeFormatted = remember(item.createdAt) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(item.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("notif_card_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!item.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = iconBg,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontWeight = if (!item.isRead) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontSize = 14.sp
                    )

                    if (!item.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.message,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = timeFormatted,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
