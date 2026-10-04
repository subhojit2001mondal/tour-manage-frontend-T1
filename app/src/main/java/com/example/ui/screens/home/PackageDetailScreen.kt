package com.example.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.Departure
import com.example.data.models.formatTimestampDate
import com.example.data.repository.TourRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageDetailScreen(
    packageId: String,
    repository: TourRepository,
    onNavigateBack: () -> Unit,
    onNavigateToBooking: (String, String) -> Unit,
    onNavigateToCompare: () -> Unit,
    onNavigateToAgency: ((String) -> Unit)? = null
) {
    val packages by repository.packages.collectAsState()
    val agencies by repository.agencies.collectAsState()
    val destinations by repository.destinations.collectAsState()
    val departures by repository.departures.collectAsState()
    val compareIds by repository.comparePackageIds.collectAsState()

    val pkg = packages.find { it.id == packageId }
    val agency = agencies.find { it.id == pkg?.agencyId }
    val destination = destinations.find { it.id == pkg?.destinationId }
    val packageDepartures = departures.filter { it.packageId == packageId }
    val isCompared = compareIds.contains(packageId)
    val wishlist by repository.wishlist.collectAsState()
    val isWishlisted = remember(wishlist, packageId) { repository.isWishlisted(packageId) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(packageId) {
        repository.addRecentlyViewed(packageId)
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Overview & Itinerary, 1: Vehicle, 2: Food, 3: Hotel
    var expandedDay by remember { mutableIntStateOf(1) }

    if (pkg == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Package Details") },
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
                Text("Package not found")
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pkg.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val (saved, item) = repository.toggleWishlist(pkg.id)
                            scope.launch {
                                val res = snackbarHostState.showSnackbar(
                                    message = if (saved) "Saved to Wishlist" else "Removed from Wishlist",
                                    actionLabel = if (!saved) "Undo" else null,
                                    duration = SnackbarDuration.Short
                                )
                                if (res == SnackbarResult.ActionPerformed && item != null) {
                                    repository.restoreWishlistItem(item)
                                }
                            }
                        },
                        modifier = Modifier.testTag("toggle_wishlist_button")
                    ) {
                        Icon(
                            imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Wishlist",
                            tint = if (isWishlisted) StatusRed else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { repository.toggleCompare(pkg.id) },
                        modifier = Modifier.testTag("toggle_compare_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                            contentDescription = "Compare",
                            tint = if (isCompared) TourGold else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Starting from",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IndianPriceText(amount = pkg.pricePerPerson, fontSize = 20)
                    }

                    Button(
                        onClick = {
                            val firstDepId = packageDepartures.firstOrNull { it.status == "open" || it.status == "limited" }?.id ?: ""
                            onNavigateToBooking(pkg.id, firstDepId)
                        },
                        modifier = Modifier.testTag("book_now_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Book Now", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Package Images
            item {
                val images = if (pkg.imageUrls.isNotEmpty()) pkg.imageUrls else listOf("https://images.unsplash.com/photo-1595815771614-ade9d652a65d?w=1000")
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(images) { img ->
                        AsyncImage(
                            model = img,
                            contentDescription = pkg.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillParentMaxWidth(0.92f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }
            }

            // Title, Rating, and Duration
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${pkg.days} Days • ${pkg.nights} Nights",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        RatingBadge(rating = pkg.rating)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = pkg.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )

                    if (destination != null) {
                        Text(
                            text = "Destination: ${destination.name}, ${destination.state}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Agency Card with verified badge and rating
            item {
                if (agency != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable(enabled = onNavigateToAgency != null) {
                                onNavigateToAgency?.invoke(agency.id)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Business,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = agency.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    VerifiedAgencyBadge(verified = agency.verified, tier = agency.tier)
                                }
                                Text(
                                    text = "Based in ${agency.city} • Verified Local Tour Operator",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RatingBadge(rating = agency.rating)
                        }
                    }
                }
            }

            // Next Departures / Upcoming Dates Carousel
            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = "Upcoming Departures",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    if (packageDepartures.isEmpty()) {
                        Text(
                            text = "Departures available on request daily. Contact support.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(packageDepartures) { dep ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = CardDefaults.outlinedCardBorder(),
                                    modifier = Modifier.clickable {
                                        onNavigateToBooking(pkg.id, dep.id)
                                    }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        DepartureStatusChip(status = dep.status)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = formatTimestampDate(dep.date),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${dep.seatsLeft} seats left",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tab bar for: 1. Overview & Itinerary, 2. Vehicle, 3. Food, 4. Hotel
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Itinerary", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Vehicle", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Food", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Hotel", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // TAB 0: ITINERARY & INCLUSIONS
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Day-by-Day Tour Itinerary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            pkg.itinerary.forEach { dayItem ->
                                val isExpanded = expandedDay == dayItem.day
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { expandedDay = if (isExpanded) 0 else dayItem.day },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    contentColor = Color.White
                                                ) {
                                                    Text(
                                                        text = "Day ${dayItem.day}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = dayItem.title,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null
                                            )
                                        }
                                        AnimatedVisibility(visible = isExpanded) {
                                            Text(
                                                text = dayItem.details,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                                lineHeight = 20.sp,
                                                modifier = Modifier.padding(top = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Inclusions
                            Text(
                                text = "What's Included",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            pkg.inclusions.forEach { inc ->
                                Row(
                                    modifier = Modifier.padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = inc, fontSize = 13.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Exclusions
                            Text(
                                text = "Exclusions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            pkg.exclusions.forEach { exc ->
                                Row(
                                    modifier = Modifier.padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = exc, fontSize = 13.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Cancellation Policy
                            Text(
                                text = "Cancellation Policy",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = pkg.cancellationPolicy.ifBlank { "Standard cancellation: Full refund up to 7 days before departure date." },
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: VEHICLE SECTION SUPPLIED BY AGENCY
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Transportation & Vehicle Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Dedicated vehicle provided exclusively by ${agency?.name ?: "the agency"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (pkg.vehicle.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = pkg.vehicle.imageUrl,
                                    contentDescription = pkg.vehicle.vehicleName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    VehicleSpecRow("Type", pkg.vehicle.type)
                                    VehicleSpecRow("Vehicle Model", pkg.vehicle.vehicleName.ifBlank { "Touring Sedan / SUV" })
                                    VehicleSpecRow("Air Conditioning", if (pkg.vehicle.ac) "Included (AC)" else "Non-AC")
                                    VehicleSpecRow("Seating Capacity", "${pkg.vehicle.seatingCapacity} Passengers")
                                    VehicleSpecRow("Station/Airport Pickup", if (pkg.vehicle.stationOrAirportPickup) "Yes, Included" else "No")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = pkg.vehicle.details.ifBlank { "Fully licensed tourist commercial vehicle with certified professional driver, fuel, all state taxes, and toll permits included." },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: FOOD SECTION SUPPLIED BY AGENCY
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Meal Plan & Dining",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Food arrangements curated by ${agency?.name ?: "the agency"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    VehicleSpecRow("Meal Plan", pkg.food.mealPlan)
                                    VehicleSpecRow("Cuisine Offered", pkg.food.cuisine)
                                    VehicleSpecRow("Jain Diet on Request", if (pkg.food.jainOnRequest) "Available on prior notice" else "Not guaranteed")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Sample Menu / Dining Details:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = pkg.food.details.ifBlank { "Breakfast includes daily hot buffet items (Indian & Continental). Dinners feature multi-course regional specialties prepared under strict hygiene standards." },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // TAB 3: HOTEL SECTION SUPPLIED BY AGENCY
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Accommodation & Hotels",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Stays verified and inspected by ${agency?.name ?: "the agency"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (pkg.hotel.imageUrls.isNotEmpty()) {
                                LazyRow(
                                    contentPadding = PaddingValues(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(pkg.hotel.imageUrls) { imgUrl ->
                                        AsyncImage(
                                            model = imgUrl,
                                            contentDescription = pkg.hotel.hotelName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(160.dp, 120.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                    }
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    VehicleSpecRow("Property Name", pkg.hotel.hotelName.ifBlank { "Selected Boutique Hotel" })
                                    VehicleSpecRow("Category", pkg.hotel.category)
                                    VehicleSpecRow("Room Type", pkg.hotel.roomType)
                                    VehicleSpecRow("Occupancy", pkg.hotel.occupancy)

                                    if (pkg.hotel.amenities.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Amenities:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = pkg.hotel.amenities.joinToString(" • "),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = pkg.hotel.details.ifBlank { "Comfortable, centrally located accommodations with private bathrooms, 24-hour hot water, and complimentary breakfast." },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun VehicleSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
