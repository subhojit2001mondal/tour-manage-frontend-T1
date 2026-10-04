package com.example.ui.screens.wishlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.TourPackage
import com.example.data.models.WishlistItem
import com.example.data.models.formatInr
import com.example.data.models.formatTimestampDate
import com.example.data.repository.TourRepository
import com.example.ui.components.EmptyState
import com.example.ui.components.ErrorBanner
import com.example.ui.components.IndianPriceText
import com.example.ui.components.RatingBadge
import com.example.ui.components.VerifiedAgencyBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    repository: TourRepository,
    onNavigateBack: () -> Unit,
    onNavigateToPackage: (String) -> Unit,
    onNavigateToBooking: (String, String) -> Unit,
    onNavigateToCompare: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val wishlistItems by repository.wishlist.collectAsState()
    val packages by repository.packages.collectAsState()
    val agencies by repository.agencies.collectAsState()
    val destinations by repository.destinations.collectAsState()
    val departures by repository.departures.collectAsState()
    val firestoreError by repository.firestoreError.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("recent") } // "recent", "price_asc", "departure"

    val selectedForCompare = remember { mutableStateListOf<String>() }

    // Enriched wishlisted packages
    val enrichedList = remember(wishlistItems, packages, destinations, agencies, departures, searchQuery, sortBy) {
        val list = wishlistItems.mapNotNull { item ->
            val pkg = packages.find { it.id == item.packageId } ?: return@mapNotNull null
            val agency = agencies.find { it.id == pkg.agencyId }
            val dest = destinations.find { it.id == pkg.destinationId }
            val nextDep = departures.firstOrNull { it.packageId == pkg.id && (it.status == "open" || it.status == "limited") }
            WishlistEnrichedItem(item, pkg, agency, dest, nextDep)
        }.filter {
            if (searchQuery.isBlank()) true else {
                val q = searchQuery.lowercase()
                it.pkg.title.lowercase().contains(q) ||
                (it.destination?.name?.lowercase()?.contains(q) ?: false) ||
                (it.agency?.name?.lowercase()?.contains(q) ?: false)
            }
        }

        when (sortBy) {
            "price_asc" -> list.sortedBy { it.pkg.pricePerPerson }
            "departure" -> list.sortedBy { it.nextDeparture?.date?.toDate()?.time ?: Long.MAX_VALUE }
            else -> list.sortedByDescending { it.wishlistItem.savedAt }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = StatusRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saved Tours (${wishlistItems.size})", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedForCompare.size >= 2) {
                        Button(
                            onClick = {
                                selectedForCompare.forEach { repository.toggleCompare(it) }
                                onNavigateToCompare()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Compare (${selectedForCompare.size})", fontSize = 12.sp)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Error banner for save or network failures
            ErrorBanner(
                errorMessage = firestoreError,
                onDismiss = { repository.clearError() },
                onRetry = { repository.refreshAll() }
            )

            // Search & Sort row
            if (wishlistItems.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter saved tours...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wishlist_search_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    var sortMenuOpen by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { sortMenuOpen = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                            DropdownMenuItem(text = { Text("Recently Saved") }, onClick = { sortBy = "recent"; sortMenuOpen = false })
                            DropdownMenuItem(text = { Text("Price: Low to High") }, onClick = { sortBy = "price_asc"; sortMenuOpen = false })
                            DropdownMenuItem(text = { Text("Soonest Departure") }, onClick = { sortBy = "departure"; sortMenuOpen = false })
                        }
                    }
                }
            }

            if (enrichedList.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.FavoriteBorder,
                    title = if (searchQuery.isNotBlank()) "No matching saved tours" else "Your Wishlist is Empty",
                    subtitle = if (searchQuery.isNotBlank()) "Try a different search keyword." else "Tap the heart icon on any package to save it here, track price drops and seat availability.",
                    actionButton = {
                        Button(onClick = onNavigateToHome) {
                            Text("Explore Packages")
                        }
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(enrichedList) { item ->
                        val isSelected = selectedForCompare.contains(item.pkg.id)

                        WishlistCard(
                            item = item,
                            isSelectedForCompare = isSelected,
                            onToggleCompareSelect = {
                                if (isSelected) selectedForCompare.remove(item.pkg.id)
                                else if (selectedForCompare.size < 4) selectedForCompare.add(item.pkg.id)
                            },
                            onRemove = {
                                val (_, removedItem) = repository.toggleWishlist(item.pkg.id)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Removed from wishlist",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed && removedItem != null) {
                                        repository.restoreWishlistItem(removedItem)
                                    }
                                }
                            },
                            onCardClick = { onNavigateToPackage(item.pkg.id) },
                            onBookNow = {
                                val depId = item.nextDeparture?.id ?: ""
                                onNavigateToBooking(item.pkg.id, depId)
                            }
                        )
                    }
                }
            }
        }
    }
}

data class WishlistEnrichedItem(
    val wishlistItem: WishlistItem,
    val pkg: TourPackage,
    val agency: com.example.data.models.Agency?,
    val destination: com.example.data.models.Destination?,
    val nextDeparture: com.example.data.models.Departure?
)

@Composable
fun WishlistCard(
    item: WishlistEnrichedItem,
    isSelectedForCompare: Boolean,
    onToggleCompareSelect: () -> Unit,
    onRemove: () -> Unit,
    onCardClick: () -> Unit,
    onBookNow: () -> Unit
) {
    val pkg = item.pkg
    val savedPrice = item.wishlistItem.savedPrice
    val currentPrice = pkg.pricePerPerson

    // Live Badge conditions
    val priceDiff = currentPrice - savedPrice
    val seatsLeft = item.nextDeparture?.seatsLeft ?: 20
    val isSoldOut = item.nextDeparture?.status == "full" || seatsLeft == 0
    val isFewSeats = seatsLeft in 1..4

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isSelectedForCompare) CardDefaults.outlinedCardBorder().copy(width = 2.dp) else CardDefaults.outlinedCardBorder()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val banner = pkg.imageUrls.firstOrNull() ?: item.destination?.coverImageUrl ?: ""
                AsyncImage(
                    model = banner.ifBlank { "https://images.unsplash.com/photo-1595815771614-ade9d652a65d?w=800" },
                    contentDescription = pkg.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top row: Compare checkbox & Remove heart
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = isSelectedForCompare,
                        onClick = onToggleCompareSelect,
                        label = { Text("Select to Compare", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )

                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xCCFFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Remove from Wishlist",
                            tint = StatusRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Live Status Badge at bottom left of image
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (priceDiff < 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFDCFCE7),
                            contentColor = StatusGreen
                        ) {
                            Text(
                                text = "Price dropped by ${formatInr(-priceDiff)}!",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (priceDiff > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFEDD5),
                            contentColor = StatusOrange
                        ) {
                            Text(
                                text = "Price increased by ${formatInr(priceDiff)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (isSoldOut) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFEE2E2),
                            contentColor = StatusRed
                        ) {
                            Text(
                                text = "Sold out",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isFewSeats) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFEDD5),
                            contentColor = StatusOrange
                        ) {
                            Text(
                                text = "Few seats left ($seatsLeft)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Body
            Column(modifier = Modifier.padding(14.dp)) {
                if (item.agency != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.agency.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        VerifiedAgencyBadge(verified = item.agency.verified, tier = item.agency.tier)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = pkg.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Summary of vehicle, meal, hotel
                Text(
                    text = "${pkg.days}D/${pkg.nights}N • ${pkg.vehicle.type} (${if (pkg.vehicle.ac) "AC" else "Non-AC"}) • ${pkg.food.mealPlan} • ${pkg.hotel.category}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (item.nextDeparture != null) {
                    Text(
                        text = "Next Departure: ${formatTimestampDate(item.nextDeparture.date)} (${item.nextDeparture.seatsLeft} seats)",
                        fontSize = 11.sp,
                        color = StatusGreen,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IndianPriceText(amount = currentPrice, fontSize = 16)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onRemove,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = StatusRed
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove", fontSize = 12.sp, color = StatusRed)
                        }

                        Button(
                            onClick = onBookNow,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text("Book Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
