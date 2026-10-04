package com.example.ui.screens.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Departure
import com.example.data.models.TourPackage
import com.example.data.models.formatInr
import com.example.data.models.formatTimestampDate
import com.example.data.repository.TourRepository
import com.example.ui.components.DepartureStatusChip
import com.example.ui.components.EmptyState
import com.example.ui.components.RatingBadge
import com.example.ui.components.VerifiedAgencyBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    repository: TourRepository,
    onNavigateToBooking: (String, String) -> Unit,
    onNavigateToHome: () -> Unit
) {
    val packages by repository.packages.collectAsState()
    val agencies by repository.agencies.collectAsState()
    val destinations by repository.destinations.collectAsState()
    val departures by repository.departures.collectAsState()
    val compareIds by repository.comparePackageIds.collectAsState()

    var selectedDestinationId by remember { mutableStateOf<String?>(null) }
    var showAddPackageDialog by remember { mutableStateOf(false) }

    // Filter packages by compare list or by selected destination
    val comparedPackages = remember(compareIds, packages, selectedDestinationId) {
        val list = packages.filter { compareIds.contains(it.id) }
        if (selectedDestinationId != null) {
            list.filter { it.destinationId == selectedDestinationId }
        } else list
    }

    // Identify lowest price and best rating
    val lowestPrice = remember(comparedPackages) {
        comparedPackages.minOfOrNull { it.pricePerPerson } ?: 0L
    }
    val bestRating = remember(comparedPackages) {
        comparedPackages.maxOfOrNull { it.rating } ?: 0.0
    }
    val wishlist by repository.wishlist.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Compare Tours (${comparedPackages.size}/4)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    if (comparedPackages.isNotEmpty()) {
                        TextButton(
                            onClick = { repository.clearCompare() },
                            modifier = Modifier.testTag("clear_compare_button")
                        ) {
                            Text("Clear")
                        }
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
            // Destination Filter & Add Package Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                var destMenuOpen by remember { mutableStateOf(false) }
                val currentDestName = destinations.find { it.id == selectedDestinationId }?.name ?: "All Destinations"

                Box {
                    OutlinedButton(
                        onClick = { destMenuOpen = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = currentDestName,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = destMenuOpen,
                        onDismissRequest = { destMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Destinations") },
                            onClick = { selectedDestinationId = null; destMenuOpen = false }
                        )
                        destinations.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d.name) },
                                onClick = { selectedDestinationId = d.id; destMenuOpen = false }
                            )
                        }
                    }
                }

                Button(
                    onClick = { showAddPackageDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Tour", fontSize = 12.sp)
                }
            }

            if (comparedPackages.isEmpty()) {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.CompareArrows,
                    title = "No tours selected for comparison",
                    subtitle = "Pick 2 to 4 packages across different agencies to compare vehicles, hotels, food and prices side-by-side.",
                    actionButton = {
                        Button(onClick = {
                            // Pick top 2-3 packages as demo
                            val sample = packages.take(3)
                            sample.forEach { repository.toggleCompare(it.id) }
                        }) {
                            Text("Compare Top Packages")
                        }
                    }
                )
            } else {
                // Side-by-side comparison table (Horizontal Scroll)
                val tableScrollState = rememberScrollState()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(tableScrollState)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            // Column Headers: Package Titles & Remove Button & Book Button
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Left spec header label spacer
                                Box(modifier = Modifier.width(130.dp)) {
                                    Text(
                                        text = "Features & Specifications",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                comparedPackages.forEach { pkg ->
                                    val agency = agencies.find { it.id == pkg.agencyId }
                                    val nextDep = departures.firstOrNull { it.packageId == pkg.id && (it.status == "open" || it.status == "limited") }

                                    Card(
                                        modifier = Modifier.width(180.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = CardDefaults.outlinedCardBorder()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text(
                                                    text = agency?.name ?: "Agency",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                val isWishlisted = wishlist.any { it.packageId == pkg.id }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(
                                                        onClick = {
                                                            val (saved, item) = repository.toggleWishlist(pkg.id)
                                                            scope.launch {
                                                                val res = snackbarHostState.showSnackbar(
                                                                    message = if (saved) "Added to wishlist" else "Removed from wishlist",
                                                                    actionLabel = "Undo",
                                                                    duration = SnackbarDuration.Short
                                                                )
                                                                if (res == SnackbarResult.ActionPerformed && item != null) {
                                                                    if (saved) {
                                                                        repository.removeFromWishlist(pkg.id)
                                                                    } else {
                                                                        repository.restoreWishlistItem(item)
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .minimumInteractiveComponentSize()
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                                            contentDescription = "Wishlist",
                                                            tint = if (isWishlisted) StatusRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { repository.removeFromCompare(pkg.id) },
                                                        modifier = Modifier.size(20.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Remove",
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = pkg.title,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                lineHeight = 16.sp
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Button(
                                                onClick = {
                                                    val depId = nextDep?.id ?: ""
                                                    onNavigateToBooking(pkg.id, depId)
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(vertical = 6.dp)
                                            ) {
                                                Text("Book", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Comparison Table Rows:
                            // 1. Agency & Tier
                            ComparisonRow("Agency", comparedPackages) { pkg ->
                                val ag = agencies.find { it.id == pkg.agencyId }
                                Column {
                                    Text(text = ag?.name ?: "Agency", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    if (ag != null) {
                                        VerifiedAgencyBadge(verified = ag.verified, tier = ag.tier)
                                    }
                                }
                            }

                            // 2. Rating (Highlight best rating)
                            ComparisonRow("Rating", comparedPackages) { pkg ->
                                val isBest = pkg.rating == bestRating
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RatingBadge(rating = pkg.rating)
                                    if (isBest) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = TourGold,
                                            contentColor = Color.Black
                                        ) {
                                            Text(
                                                text = "BEST",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. Price per person (Highlight lowest price)
                            ComparisonRow("Price / Person", comparedPackages) { pkg ->
                                val isLowest = pkg.pricePerPerson == lowestPrice
                                Column {
                                    Text(
                                        text = formatInr(pkg.pricePerPerson),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isLowest) StatusGreen else MaterialTheme.colorScheme.primary
                                    )
                                    if (isLowest) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFDCFCE7),
                                            contentColor = StatusGreen
                                        ) {
                                            Text(
                                                text = "LOWEST PRICE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 4. Duration
                            ComparisonRow("Duration", comparedPackages) { pkg ->
                                Text(text = "${pkg.days}D / ${pkg.nights}N", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }

                            // 5. Vehicle Type & Model
                            ComparisonRow("Vehicle", comparedPackages) { pkg ->
                                Column {
                                    Text(text = pkg.vehicle.type, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(text = pkg.vehicle.vehicleName.ifBlank { "Dedicated Cab" }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            // 6. Air Conditioning (AC)
                            ComparisonRow("AC / Non-AC", comparedPackages) { pkg ->
                                Text(
                                    text = if (pkg.vehicle.ac) "✓ Air Conditioned" else "Non-AC",
                                    fontSize = 12.sp,
                                    color = if (pkg.vehicle.ac) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // 7. Meal Plan
                            ComparisonRow("Meal Plan", comparedPackages) { pkg ->
                                Text(text = pkg.food.mealPlan, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                            }

                            // 8. Cuisine
                            ComparisonRow("Cuisine", comparedPackages) { pkg ->
                                Text(text = pkg.food.cuisine, fontSize = 12.sp)
                            }

                            // 9. Hotel Name & Category
                            ComparisonRow("Hotel", comparedPackages) { pkg ->
                                Column {
                                    Text(text = pkg.hotel.category, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(text = pkg.hotel.hotelName.ifBlank { "Selected Hotel" }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            // 10. Room Type
                            ComparisonRow("Room Type", comparedPackages) { pkg ->
                                Text(text = pkg.hotel.roomType, fontSize = 12.sp)
                            }

                            // 11. Key Inclusions
                            ComparisonRow("Top Inclusions", comparedPackages) { pkg ->
                                Column {
                                    pkg.inclusions.take(3).forEach { inc ->
                                        Text(text = "• $inc", fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }

                            // 12. Cancellation Policy
                            ComparisonRow("Cancellation", comparedPackages) { pkg ->
                                Text(
                                    text = pkg.cancellationPolicy.ifBlank { "Standard refund policy" },
                                    fontSize = 11.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // 13. Next Available Date & Seats Left
                            ComparisonRow("Next Departure", comparedPackages) { pkg ->
                                val nextDep = departures.firstOrNull { it.packageId == pkg.id && (it.status == "open" || it.status == "limited") }
                                if (nextDep != null) {
                                    Column {
                                        Text(text = formatTimestampDate(nextDep.date), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(text = "${nextDep.seatsLeft} seats left", fontSize = 11.sp, color = StatusGreen)
                                        DepartureStatusChip(status = nextDep.status)
                                    }
                                } else {
                                    Text(text = "Inquire for dates", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Spacer(modifier = Modifier.height(30.dp))
                        }
                    }
                }
            }
        }
    }

    // Add Package Dialog
    if (showAddPackageDialog) {
        val availableToAdd = packages.filter { !compareIds.contains(it.id) }
        AlertDialog(
            onDismissRequest = { showAddPackageDialog = false },
            title = { Text("Add Tour to Compare") },
            text = {
                if (availableToAdd.isEmpty()) {
                    Text("All packages are already added to comparison.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                        items(availableToAdd) { p ->
                            val ag = agencies.find { it.id == p.agencyId }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        repository.toggleCompare(p.id)
                                        showAddPackageDialog = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(text = p.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = "${ag?.name ?: "Agency"} • ${formatInr(p.pricePerPerson)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddPackageDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ComparisonRow(
    label: String,
    packages: List<TourPackage>,
    content: @Composable (TourPackage) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Label Header
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Columns for each package
            packages.forEach { pkg ->
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    content(pkg)
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    }
}
