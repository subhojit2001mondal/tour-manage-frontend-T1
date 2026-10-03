package com.example.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.Agency
import com.example.data.models.Destination
import com.example.data.models.TourPackage
import com.example.data.repository.TourRepository
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: TourRepository,
    onNavigateToDestination: (String) -> Unit,
    onNavigateToPackage: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToCompare: () -> Unit
) {
    val destinations by repository.destinations.collectAsState()
    val agencies by repository.agencies.collectAsState()
    val packages by repository.packages.collectAsState()
    val compareIds by repository.comparePackageIds.collectAsState()
    val firestoreError by repository.firestoreError.collectAsState()
    val isLoading by repository.isLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("All") }
    var showFilterSheet by remember { mutableStateOf(false) }

    // Filter states
    var maxBudget by remember { mutableStateOf(50000f) }
    var minDuration by remember { mutableStateOf(0) }
    var selectedAgencyId by remember { mutableStateOf<String?>(null) }
    var minRating by remember { mutableStateOf(0.0) }
    var selectedHotelCategory by remember { mutableStateOf<String?>(null) }
    var selectedVehicleType by remember { mutableStateOf<String?>(null) }
    var selectedMealPlan by remember { mutableStateOf<String?>(null) }
    var selectedCuisine by remember { mutableStateOf<String?>(null) }
    var sortBy by remember { mutableStateOf("rating") } // "rating", "price_asc", "price_desc"

    val regions = listOf("All", "North", "South", "East", "West", "Central", "Northeast", "Islands")

    // Filtered & sorted packages
    val filteredPackages = remember(packages, searchQuery, selectedRegion, maxBudget, minDuration, selectedAgencyId, minRating, selectedHotelCategory, selectedVehicleType, selectedMealPlan, selectedCuisine, sortBy) {
        packages.filter { pkg ->
            val dest = destinations.find { it.id == pkg.destinationId }
            val ag = agencies.find { it.id == pkg.agencyId }

            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.lowercase()
                pkg.title.lowercase().contains(q) ||
                (dest?.name?.lowercase()?.contains(q) ?: false) ||
                (dest?.state?.lowercase()?.contains(q) ?: false) ||
                (dest?.tags?.any { it.lowercase().contains(q) } ?: false) ||
                (ag?.name?.lowercase()?.contains(q) ?: false)
            }

            val matchesRegion = if (selectedRegion == "All") true else {
                dest?.region.equals(selectedRegion, ignoreCase = true)
            }

            val matchesBudget = pkg.pricePerPerson <= maxBudget.toLong()
            val matchesDuration = if (minDuration > 0) pkg.days >= minDuration else true
            val matchesAgency = if (selectedAgencyId != null) pkg.agencyId == selectedAgencyId else true
            val matchesRating = pkg.rating >= minRating
            val matchesHotel = if (selectedHotelCategory != null) pkg.hotel.category.equals(selectedHotelCategory, ignoreCase = true) else true
            val matchesVehicle = if (selectedVehicleType != null) pkg.vehicle.type.equals(selectedVehicleType, ignoreCase = true) else true
            val matchesMeal = if (selectedMealPlan != null) pkg.food.mealPlan.equals(selectedMealPlan, ignoreCase = true) else true
            val matchesCuisine = if (selectedCuisine != null) pkg.food.cuisine.contains(selectedCuisine!!, ignoreCase = true) else true

            matchesSearch && matchesRegion && matchesBudget && matchesDuration && matchesAgency && matchesRating && matchesHotel && matchesVehicle && matchesMeal && matchesCuisine
        }.let { list ->
            when (sortBy) {
                "price_asc" -> list.sortedBy { it.pricePerPerson }
                "price_desc" -> list.sortedByDescending { it.pricePerPerson }
                else -> list.sortedByDescending { it.rating }
            }
        }
    }

    // Suggestions for search
    val searchSuggestions = remember(searchQuery, destinations, agencies, packages) {
        if (searchQuery.length < 2) emptyList()
        else {
            val q = searchQuery.lowercase()
            val destNames = destinations.filter { it.name.lowercase().contains(q) || it.state.lowercase().contains(q) }.map { "${it.name}, ${it.state}" }
            val agencyNames = agencies.filter { it.name.lowercase().contains(q) }.map { it.name }
            val pkgTitles = packages.filter { it.title.lowercase().contains(q) }.map { it.title }
            (destNames + agencyNames + pkgTitles).distinct().take(4)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = "Tour Manage",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Tour Manage",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 19.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "India's Tour Marketplace",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier.testTag("profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = "Customer Profile"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Error banner for Firebase failures
            item {
                ErrorBanner(
                    errorMessage = firestoreError,
                    onDismiss = { repository.clearError() },
                    onRetry = { repository.refreshAll() }
                )
            }

            // Search Header Box
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_bar"),
                        placeholder = { Text("Search places, states, packages, agencies...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                                IconButton(
                                    onClick = { showFilterSheet = true },
                                    modifier = Modifier.testTag("filter_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Filters",
                                        tint = if (selectedAgencyId != null || minDuration > 0 || maxBudget < 50000f || minRating > 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    )

                    // As-you-type search suggestions
                    if (searchSuggestions.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 4.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column {
                                searchSuggestions.forEach { suggestion ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { searchQuery = suggestion }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = suggestion,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Region Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    regions.forEach { region ->
                        val isSelected = selectedRegion == region
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedRegion = region },
                            label = { Text(region, fontSize = 12.sp) },
                            modifier = Modifier.padding(end = 8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // Featured Destinations Section
            if (searchQuery.isBlank() && selectedRegion == "All") {
                item {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Featured Destinations",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "All India",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        val featuredDests = destinations.filter { it.featured || it.isDemo }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(featuredDests) { dest ->
                                DestinationHeroCard(
                                    destination = dest,
                                    onClick = { onNavigateToDestination(dest.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Packages Header with Sort & Count
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tour Packages (${filteredPackages.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Quick sort menu
                    var sortMenuOpen by remember { mutableStateOf(false) }
                    Box {
                        TextButton(
                            onClick = { sortMenuOpen = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (sortBy) {
                                    "price_asc" -> "Price: Low to High"
                                    "price_desc" -> "Price: High to Low"
                                    else -> "Top Rated"
                                },
                                fontSize = 12.sp
                            )
                        }
                        DropdownMenu(
                            expanded = sortMenuOpen,
                            onDismissRequest = { sortMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Top Rated") },
                                onClick = { sortBy = "rating"; sortMenuOpen = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Price: Low to High") },
                                onClick = { sortBy = "price_asc"; sortMenuOpen = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Price: High to Low") },
                                onClick = { sortBy = "price_desc"; sortMenuOpen = false }
                            )
                        }
                    }
                }
            }

            // Package Cards List
            if (filteredPackages.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.TravelExplore,
                        title = "No packages match your search",
                        subtitle = "Try adjusting your budget, region, or vehicle filters to see all available India tours.",
                        actionButton = {
                            Button(onClick = {
                                searchQuery = ""
                                selectedRegion = "All"
                                maxBudget = 50000f
                                minDuration = 0
                                selectedAgencyId = null
                                minRating = 0.0
                                selectedHotelCategory = null
                                selectedVehicleType = null
                                selectedMealPlan = null
                                selectedCuisine = null
                            }) {
                                Text("Reset All Filters")
                            }
                        }
                    )
                }
            } else {
                items(filteredPackages) { pkg ->
                    val agency = agencies.find { it.id == pkg.agencyId }
                    val dest = destinations.find { it.id == pkg.destinationId }
                    val isCompared = compareIds.contains(pkg.id)

                    TourPackageCard(
                        pkg = pkg,
                        agency = agency,
                        destination = dest,
                        isCompared = isCompared,
                        onCompareToggle = {
                            val added = repository.toggleCompare(pkg.id)
                            if (added && compareIds.size >= 2) {
                                // show shortcut or indicator
                            }
                        },
                        onClick = { onNavigateToPackage(pkg.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter Tours",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        maxBudget = 50000f
                        minDuration = 0
                        selectedAgencyId = null
                        minRating = 0.0
                        selectedHotelCategory = null
                        selectedVehicleType = null
                        selectedMealPlan = null
                        selectedCuisine = null
                    }) {
                        Text("Reset")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Budget Slider
                Text(
                    text = "Budget up to: ₹${maxBudget.toInt()}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Slider(
                    value = maxBudget,
                    onValueChange = { maxBudget = it },
                    valueRange = 10000f..75000f,
                    steps = 12
                )

                // Minimum Rating
                Text(text = "Rating", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    listOf(0.0 to "Any", 4.0 to "4.0+", 4.5 to "4.5+", 4.8 to "4.8+").forEach { (r, label) ->
                        FilterChip(
                            selected = minRating == r,
                            onClick = { minRating = r },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }

                // Vehicle Type
                Text(text = "Vehicle Type", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                ) {
                    listOf(null to "Any", "Sedan" to "Sedan", "SUV" to "SUV", "Tempo Traveller" to "Tempo", "4x4 Jeep" to "Jeep").forEach { (v, label) ->
                        FilterChip(
                            selected = selectedVehicleType == v,
                            onClick = { selectedVehicleType = v },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }

                // Hotel Category
                Text(text = "Hotel Category", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                ) {
                    listOf(null to "Any", "Budget" to "Budget", "3-star" to "3-Star", "4-star" to "4-Star", "Heritage" to "Heritage", "Resort" to "Resort").forEach { (h, label) ->
                        FilterChip(
                            selected = selectedHotelCategory == h,
                            onClick = { selectedHotelCategory = h },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }

                // Food / Diet
                Text(text = "Cuisine / Diet", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    listOf(null to "Any", "Veg" to "Pure Veg", "Non-veg" to "Non-Veg").forEach { (c, label) ->
                        FilterChip(
                            selected = selectedCuisine == c,
                            onClick = { selectedCuisine = c },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Filters (${filteredPackages.size} Tours)")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun DestinationHeroCard(
    destination: Destination,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .height(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = destination.coverImageUrl.ifBlank { "https://images.unsplash.com/photo-1595815771614-ade9d652a65d?w=600" },
                contentDescription = destination.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC000000)),
                            startY = 60f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = TourNavy.copy(alpha = 0.85f),
                    contentColor = Color.White
                ) {
                    Text(
                        text = destination.region,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = destination.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = destination.state,
                    color = Color(0xFFE2E8F0),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun TourPackageCard(
    pkg: TourPackage,
    agency: Agency?,
    destination: Destination?,
    isCompared: Boolean,
    onCompareToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("package_card_${pkg.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Image banner with duration and compare toggle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val bannerUrl = pkg.imageUrls.firstOrNull() ?: destination?.coverImageUrl ?: ""
                AsyncImage(
                    model = bannerUrl.ifBlank { "https://images.unsplash.com/photo-1595815771614-ade9d652a65d?w=800" },
                    contentDescription = pkg.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0x55000000), Color.Transparent, Color(0xAA000000))
                            )
                        )
                )

                // Duration badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xCC000000),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = TourGold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${pkg.days}D / ${pkg.nights}N",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Add to compare icon button
                IconButton(
                    onClick = onCompareToggle,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .background(
                            if (isCompared) TourGold else Color(0x88000000),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = "Compare Package",
                        tint = if (isCompared) Color.Black else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Rating at bottom right of image
                RatingBadge(
                    rating = pkg.rating,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                )
            }

            // Body Content
            Column(modifier = Modifier.padding(14.dp)) {
                // Agency Info row
                if (agency != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = agency.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        VerifiedAgencyBadge(verified = agency.verified, tier = agency.tier)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = pkg.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Three key badges: Vehicle, Food, Hotel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = pkg.vehicle.type + if (pkg.vehicle.ac) " (AC)" else "",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = pkg.food.mealPlan,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hotel,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = pkg.hotel.category,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(10.dp))

                // Price and Explore button row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IndianPriceText(amount = pkg.pricePerPerson)

                    Button(
                        onClick = onClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("View Details", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
