package com.example.ui.screens.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Booking
import com.example.data.models.formatInr
import com.example.data.models.formatTimestampDate
import com.example.data.repository.AuthRepository
import com.example.data.repository.TourRepository
import com.example.ui.components.EmptyState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTripsScreen(
    repository: TourRepository,
    authRepo: AuthRepository,
    onNavigateToAuth: () -> Unit,
    onNavigateToChatWithBooking: (String) -> Unit,
    onNavigateToHome: () -> Unit
) {
    val currentUser by authRepo.currentUser.collectAsState()
    val bookings by repository.myBookings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Luggage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("My Trips & Bookings", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (currentUser == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                EmptyState(
                    icon = Icons.Outlined.Lock,
                    title = "Sign In to Access Your Trips",
                    subtitle = "Log in with your Gmail or email account to see all your confirmed vouchers and upcoming departures.",
                    actionButton = {
                        Button(
                            onClick = onNavigateToAuth,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("login_from_my_trips_button")
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log In / Sign Up")
                        }
                    }
                )
            }
        } else if (bookings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                EmptyState(
                    icon = Icons.Outlined.Luggage,
                    title = "No Bookings Yet",
                    subtitle = "When you book a holiday package across India, your confirmed itinerary and agent contact will appear here.",
                    actionButton = {
                        Button(
                            onClick = onNavigateToHome,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Discover Tours")
                        }
                    }
                )
            }
        } else {
            var bookingQuery by remember { mutableStateOf("") }
            var selectedBookingStatus by remember { mutableStateOf("All") }

            val filteredBookings = remember(bookings, bookingQuery, selectedBookingStatus) {
                bookings.filter { bk ->
                    val matchQuery = if (bookingQuery.isBlank()) true else {
                        val q = bookingQuery.lowercase()
                        bk.bookingCode.lowercase().contains(q) ||
                        bk.destinationName.lowercase().contains(q) ||
                        bk.agencyName.lowercase().contains(q) ||
                        bk.packageTitle.lowercase().contains(q)
                    }
                    val matchStatus = if (selectedBookingStatus == "All") true else {
                        bk.status.equals(selectedBookingStatus, ignoreCase = true)
                    }
                    matchQuery && matchStatus
                }.sortedByDescending { it.travelDate?.toDate()?.time ?: it.createdAt?.toDate()?.time ?: 0L }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search box and status filters at top of My Trips
                item {
                    Column(modifier = Modifier.padding(bottom = 6.dp)) {
                        OutlinedTextField(
                            value = bookingQuery,
                            onValueChange = { bookingQuery = it },
                            placeholder = { Text("Search by code, place, agency...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (bookingQuery.isNotEmpty()) {
                                    IconButton(onClick = { bookingQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = null)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("my_trips_search_field"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                                .padding(top = 8.dp)
                        ) {
                            listOf("All", "confirmed", "held", "completed", "cancelled").forEach { st ->
                                val isSelected = selectedBookingStatus.equals(st, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedBookingStatus = st },
                                    label = { Text(st.replaceFirstChar { it.uppercase() }) },
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                        }
                    }
                }

                if (filteredBookings.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.SearchOff,
                            title = "No matching bookings found",
                            subtitle = "Try changing your status filter or search keyword."
                        )
                    }
                } else {
                    items(filteredBookings) { bk ->
                        BookingItemCard(
                            booking = bk,
                            onChatClick = { onNavigateToChatWithBooking(bk.bookingCode) }
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

@Composable
fun BookingItemCard(
    booking: Booking,
    onChatClick: () -> Unit
) {
    val (statusBg, statusFg) = when (booking.status.lowercase()) {
        "confirmed" -> Color(0xFFDCFCE7) to StatusGreen
        "held" -> Color(0xFFFFEDD5) to StatusOrange
        "completed" -> Color(0xFFE0EFFF) to TourNavy
        else -> Color(0xFFFEE2E2) to StatusRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("booking_card_${booking.bookingCode}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Code and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ConfirmationNumber,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = booking.bookingCode,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusBg,
                    contentColor = statusFg
                ) {
                    Text(
                        text = booking.status.replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = booking.packageTitle.ifBlank { "Tour Package" },
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            if (booking.agencyName.isNotBlank()) {
                Text(
                    text = "Agency: ${booking.agencyName}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Travel Date: ${formatTimestampDate(booking.travelDate)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (booking.travelers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${booking.travelers.size} Travelers (${booking.travelers.joinToString { it.name }})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Total Paid", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatInr(booking.totalAmount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Button(
                    onClick = onChatClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Chat about this booking", fontSize = 12.sp)
                }
            }
        }
    }
}
