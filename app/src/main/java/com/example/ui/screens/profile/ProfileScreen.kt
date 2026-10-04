package com.example.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.NotificationPreferences
import com.example.data.repository.AuthRepository
import com.example.data.repository.TourRepository
import com.example.ui.theme.TourNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authRepo: AuthRepository,
    tourRepo: TourRepository,
    onNavigateBack: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToWishlist: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by authRepo.currentUser.collectAsState()
    val profile by authRepo.customerProfile.collectAsState()
    val wishlist by tourRepo.wishlist.collectAsState()
    val notifications by tourRepo.notifications.collectAsState()
    val notifPrefs by tourRepo.notificationPreferences.collectAsState()

    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }
    val firestoreSaveState by authRepo.firestoreSaveState.collectAsState()
    val customerDocExists by authRepo.customerDocExists.collectAsState()

    val initials = remember(profile?.name, currentUser?.displayName, currentUser?.email) {
        val rawName = profile?.name?.ifBlank { null }
            ?: currentUser?.displayName?.ifBlank { null }
            ?: currentUser?.email?.substringBefore("@")
            ?: "T"
        val parts = rawName.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (parts.size >= 2) {
            "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
        } else {
            rawName.take(2).uppercase()
        }
    }

    var isEditing by remember { mutableStateOf(false) }
    var editName by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var editPhone by remember(profile) { mutableStateOf(profile?.phone ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentUser != null) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = initials,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text("Profile & Settings", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToNotifications,
                        modifier = Modifier.testTag("profile_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge { Text(unreadCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (currentUser == null) {
                // Not logged in
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(54.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Guest Traveler",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Sign in to synchronize your bookings, saved trips, wishlist alerts, and support chat history.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onNavigateToAuth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Log In or Sign Up")
                        }
                    }
                }
            } else {
                // Logged in user header with avatar (initials) and green "Logged in" chip
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            modifier = Modifier.size(84.dp),
                            shadowElevation = 2.dp
                        ) {
                            val photoUrl = currentUser?.photoUrl?.toString()
                            if (!photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = initials,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = profile?.name?.ifBlank { currentUser?.displayName ?: "Traveler" } ?: "Traveler",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            text = currentUser?.email ?: "",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!profile?.phone.isNullOrBlank()) {
                            Text(
                                text = "+91 ${profile?.phone}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Green "Logged in" chip as requested
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFDCFCE7),
                            contentColor = Color(0xFF15803D),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                            modifier = Modifier.testTag("logged_in_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF16A34A), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Logged in",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Database Persistence Status Banner (Requirement 4)
                item {
                    val isSaved = customerDocExists || firestoreSaveState is com.example.data.repository.FirestoreCustomerSaveState.Saved
                    val isError = firestoreSaveState is com.example.data.repository.FirestoreCustomerSaveState.Error
                    val isSaving = firestoreSaveState is com.example.data.repository.FirestoreCustomerSaveState.Saving

                    if (isSaved) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("saved_in_database_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Saved in database ✓",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF15803D)
                                    )
                                    Text(
                                        text = "Stored at customers/${currentUser?.uid} in named database",
                                        fontSize = 11.sp,
                                        color = Color(0xFF166534)
                                    )
                                }
                            }
                        }
                    } else if (isError) {
                        val errMsg = (firestoreSaveState as com.example.data.repository.FirestoreCustomerSaveState.Error).message
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("database_save_failed_card")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Logged in, but saving to the database failed",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = errMsg,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { authRepo.retrySaveCustomerToFirestore() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry Save to Database")
                                }
                            }
                        }
                    } else if (isSaving) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Saving customer to database...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Shortcuts: Wishlist, Notifications, Activity & History
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            ProfileShortcutItem(
                                icon = Icons.Default.Favorite,
                                title = "Saved Wishlist",
                                subtitle = "${wishlist.size} packages saved",
                                badgeText = if (wishlist.isNotEmpty()) "${wishlist.size}" else null,
                                onClick = onNavigateToWishlist
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            ProfileShortcutItem(
                                icon = Icons.Default.Notifications,
                                title = "Notifications",
                                subtitle = "Price drops, booking updates & reminders",
                                badgeText = if (unreadCount > 0) "$unreadCount new" else null,
                                onClick = onNavigateToNotifications
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            ProfileShortcutItem(
                                icon = Icons.Default.History,
                                title = "Activity & History",
                                subtitle = "Past searches, bookings & recently viewed",
                                onClick = onNavigateToHistory
                            )
                        }
                    }
                }

                // Account Details Card (Name, Email, Phone editable through API)
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Contact & Travel Details",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (!isEditing) {
                                    TextButton(onClick = { isEditing = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (isEditing) {
                                OutlinedTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    label = { Text("Full Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = editPhone,
                                    onValueChange = {
                                        val clean = it.filter { ch -> ch.isDigit() }
                                        if (clean.length <= 10) editPhone = clean
                                    },
                                    label = { Text("Mobile Number (+91)") },
                                    prefix = { Text("+91 ") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { isEditing = false }) { Text("Cancel") }
                                    Button(
                                        onClick = {
                                            authRepo.updateProfileInfo(editName, editPhone)
                                            isEditing = false
                                            Toast.makeText(context, "Contact info updated", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Save Changes")
                                    }
                                }
                            } else {
                                ProfileDetailRow(icon = Icons.Default.Person, label = "Full Name", value = profile?.name?.ifBlank { "Not provided" } ?: "Not provided")
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                ProfileDetailRow(icon = Icons.Default.Email, label = "Email Address", value = currentUser?.email ?: "None")
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                ProfileDetailRow(
                                    icon = Icons.Default.Phone,
                                    label = "Mobile Number",
                                    value = if (profile?.phone.isNullOrBlank()) "Required for booking (Add now)" else "+91 ${profile?.phone}"
                                )
                            }
                        }
                    }
                }

                // Notification Settings Toggles
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Notification Preferences",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            NotificationToggleRow(
                                title = "Booking Updates",
                                subtitle = "Confirmed holds, status changes and vouchers",
                                checked = notifPrefs.bookingUpdates,
                                onCheckedChange = { tourRepo.updateNotificationPreferences(notifPrefs.copy(bookingUpdates = it)) }
                            )
                            HorizontalDivider()
                            NotificationToggleRow(
                                title = "Chat & Support Messages",
                                subtitle = "Replies from support specialists & AI assistant",
                                checked = notifPrefs.chatMessages,
                                onCheckedChange = { tourRepo.updateNotificationPreferences(notifPrefs.copy(chatMessages = it)) }
                            )
                            HorizontalDivider()
                            NotificationToggleRow(
                                title = "Price Drops & Seat Alerts",
                                subtitle = "Notifies when wishlist package prices drop or seats run low",
                                checked = notifPrefs.priceAlerts,
                                onCheckedChange = { tourRepo.updateNotificationPreferences(notifPrefs.copy(priceAlerts = it)) }
                            )
                            HorizontalDivider()
                            NotificationToggleRow(
                                title = "Trip Reminders",
                                subtitle = "Alerts 3 days and 1 day prior to travel departure",
                                checked = notifPrefs.tripReminders,
                                onCheckedChange = { tourRepo.updateNotificationPreferences(notifPrefs.copy(tripReminders = it)) }
                            )
                        }
                    }
                }

                // Log out button
                item {
                    OutlinedButton(
                        onClick = {
                            authRepo.logOut()
                            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("logout_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Default.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Out")
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

@Composable
fun ProfileShortcutItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (badgeText != null) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(text = badgeText, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ProfileDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
