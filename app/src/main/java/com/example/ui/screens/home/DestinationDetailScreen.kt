package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.repository.TourRepository
import com.example.ui.components.EmptyState
import com.example.ui.theme.TourGold
import com.example.ui.theme.TourNavy
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationDetailScreen(
    destinationId: String,
    repository: TourRepository,
    onNavigateBack: () -> Unit,
    onNavigateToPackage: (String) -> Unit,
    onNavigateToCompare: () -> Unit,
    onNavigateToCalendar: () -> Unit
) {
    val destinations by repository.destinations.collectAsState()
    val agencies by repository.agencies.collectAsState()
    val packages by repository.packages.collectAsState()
    val compareIds by repository.comparePackageIds.collectAsState()
    val wishlist by repository.wishlist.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val destination = destinations.find { it.id == destinationId }
    val destinationPackages = packages.filter { it.destinationId == destinationId }

    if (destination == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Destination Details") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Destination not found")
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(destination.name) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCompare) {
                        Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = "Compare")
                    }
                    IconButton(onClick = onNavigateToCalendar) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Hero Gallery Carousel
            item {
                val gallery = if (destination.galleryUrls.isNotEmpty()) {
                    destination.galleryUrls
                } else if (destination.coverImageUrl.isNotBlank()) {
                    listOf(destination.coverImageUrl)
                } else {
                    listOf("https://images.unsplash.com/photo-1595815771614-ade9d652a65d?w=1200")
                }

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(gallery) { imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = destination.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillParentMaxWidth(0.9f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }
            }

            // Quick Info & Tags
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = destination.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${destination.state} • ${destination.region} Region",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Best Season Card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = TourGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Best Season to Visit",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = destination.bestSeason.ifBlank { "October to May" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tags
                    if (destination.tags.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            destination.tags.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Description
                    Text(
                        text = "About this Destination",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = destination.description,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons: Compare & View Dates
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateToCompare,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("compare_packages_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compare Tours")
                        }

                        Button(
                            onClick = onNavigateToCalendar,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("view_dates_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Dates")
                        }
                    }
                }
            }

            // All Packages for this Destination
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available Tour Packages (${destinationPackages.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (destinationPackages.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Luggage,
                        title = "No packages available yet",
                        subtitle = "Agencies are updating departures for this destination."
                    )
                }
            } else {
                items(destinationPackages) { pkg ->
                    val agency = agencies.find { it.id == pkg.agencyId }
                    val isCompared = compareIds.contains(pkg.id)
                    val isWishlisted = wishlist.any { it.packageId == pkg.id }

                    TourPackageCard(
                        pkg = pkg,
                        agency = agency,
                        destination = destination,
                        isCompared = isCompared,
                        isWishlisted = isWishlisted,
                        onWishlistToggle = {
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
                        onCompareToggle = { repository.toggleCompare(pkg.id) },
                        onClick = { onNavigateToPackage(pkg.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
