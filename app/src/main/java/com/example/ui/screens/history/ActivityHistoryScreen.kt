package com.example.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Booking
import com.example.data.models.RecentlyViewedItem
import com.example.data.models.SearchHistoryItem
import com.example.data.repository.TourRepository
import com.example.ui.components.EmptyState
import com.example.ui.screens.home.TourPackageCard
import com.example.ui.screens.trips.BookingItemCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityHistoryScreen(
    repository: TourRepository,
    initialTab: Int = 0,
    onNavigateBack: () -> Unit,
    onNavigateToPackage: (String) -> Unit,
    onNavigateToSearchQuery: (String) -> Unit,
    onNavigateToChatWithBooking: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) } // 0: Bookings, 1: Searches, 2: Recently viewed

    val bookings by repository.myBookings.collectAsState()
    val searchHistory by repository.searchHistory.collectAsState()
    val recentlyViewed by repository.recentlyViewed.collectAsState()
    val packages by repository.packages.collectAsState()
    val agencies by repository.agencies.collectAsState()
    val destinations by repository.destinations.collectAsState()
    val compareIds by repository.comparePackageIds.collectAsState()
    val wishlist by repository.wishlist.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Bookings tab filters
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

    // Recently viewed packages
    val recentPackages = remember(recentlyViewed, packages) {
        recentlyViewed.mapNotNull { item ->
            packages.find { it.id == item.packageId }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity & History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs: Bookings, Searches, Recently viewed
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Bookings (${bookings.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Searches (${searchHistory.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Recent (${recentPackages.size})", fontWeight = FontWeight.SemiBold) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // TAB 0: BOOKINGS WITH SEARCH & STATUS CHIPS
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))
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
                                .testTag("booking_search_field"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Status filter chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 8.dp)
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

                        if (filteredBookings.isEmpty()) {
                            EmptyState(
                                icon = Icons.Outlined.Luggage,
                                title = "No matching bookings found",
                                subtitle = "Try changing your status filter or search keyword."
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(filteredBookings) { bk ->
                                    BookingItemCard(
                                        booking = bk,
                                        onChatClick = { onNavigateToChatWithBooking(bk.bookingCode) }
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: SEARCHES (last 20, tap to repeat, delete, clear all)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Searches",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (searchHistory.isNotEmpty()) {
                                TextButton(onClick = { repository.clearSearchHistory() }) {
                                    Text("Clear All")
                                }
                            }
                        }

                        if (searchHistory.isEmpty()) {
                            EmptyState(
                                icon = Icons.Outlined.Search,
                                title = "No search history",
                                subtitle = "Keywords and destinations you look up will be recorded here for fast access."
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(searchHistory) { item ->
                                    val timeStr = remember(item.timestamp) {
                                        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
                                    }
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onNavigateToSearchQuery(item.query) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = CardDefaults.outlinedCardBorder()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.query,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = timeStr,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = { repository.removeSearchItem(item.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Delete",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: RECENTLY VIEWED (last 20 packages with Clear all)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recently Viewed Packages",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (recentPackages.isNotEmpty()) {
                                TextButton(onClick = { repository.clearRecentlyViewed() }) {
                                    Text("Clear All")
                                }
                            }
                        }

                        if (recentPackages.isEmpty()) {
                            EmptyState(
                                icon = Icons.Outlined.Visibility,
                                title = "No recently viewed tours",
                                subtitle = "Packages you explore will show up here so you can easily compare and review."
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(recentPackages) { pkg ->
                                    val ag = agencies.find { it.id == pkg.agencyId }
                                    val dest = destinations.find { it.id == pkg.destinationId }
                                    val isCompared = compareIds.contains(pkg.id)

                                    TourPackageCard(
                                        pkg = pkg,
                                        agency = ag,
                                        destination = dest,
                                        isCompared = isCompared,
                                        onCompareToggle = { repository.toggleCompare(pkg.id) },
                                        onClick = { onNavigateToPackage(pkg.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
