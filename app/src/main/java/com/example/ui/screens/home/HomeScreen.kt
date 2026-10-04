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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.data.models.Agency
import com.example.data.models.Destination
import com.example.data.models.TourPackage
import com.example.data.repository.TourRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: TourRepository,
    onNavigateToDestination: (String) -> Unit,
    onNavigateToAgency: (String) -> Unit,
    onNavigateToPackage: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToCompare: () -> Unit,
    onNavigateToWishlist: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val destinations by repository.destinations.collectAsState()
    val agencies by repository.agencies.collectAsState()
    val packages by repository.packages.collectAsState()
    val compareIds by repository.comparePackageIds.collectAsState()
    val wishlist by repository.wishlist.collectAsState()
    val notifications by repository.notifications.collectAsState()
    val firestoreError by repository.firestoreError.collectAsState()
    val currentUser by repository.authRepo.currentUser.collectAsState()
    val customerProfile by repository.authRepo.customerProfile.collectAsState()

    val userInitials = remember(customerProfile, currentUser) {
        val name = customerProfile?.name?.ifBlank { null }
            ?: currentUser?.displayName?.ifBlank { null }
            ?: currentUser?.email?.substringBefore("@")
            ?: "T"
        val parts = name.trim().split("\\s+".toRegex())
        if (parts.size >= 2) {
            "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
        } else {
            name.take(2).uppercase()
        }
    }

    val unreadNotifCount = remember(notifications) { notifications.count { !it.isRead } }

    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("All") }
    var searchCategoryFilter by remember { mutableStateOf("All") } // "All", "Places", "Agencies", "Packages"
    var showFilterSheet by remember { mutableStateOf(false) }

    // Debounce search query by 250 ms
    LaunchedEffect(searchQuery) {
        delay(250L)
        debouncedQuery = searchQuery.trim()
        if (debouncedQuery.length >= 3) {
            repository.addSearchQuery(debouncedQuery)
        }
    }

    // Filter states for packages
    var maxBudget by remember { mutableStateOf(50000f) }
    var minDuration by remember { mutableStateOf(0) }
    var selectedAgencyId by remember { mutableStateOf<String?>(null) }
    var minRating by remember { mutableStateOf(0.0) }
    var selectedHotelCategory by remember { mutableStateOf<String?>(null) }
    var selectedVehicleType by remember { mutableStateOf<String?>(null) }
    var selectedMealPlan by remember { mutableStateOf<String?>(null) }
    var selectedCuisine by remember { mutableStateOf<String?>(null) }
    var sortBy by remember { mutableStateOf("rating") }

    val regions = listOf("All", "North", "South", "East", "West", "Central", "Northeast", "Islands")
    val isSearching = debouncedQuery.isNotBlank()

    // 1. Matched Agencies
    val matchedAgencies = remember(agencies, packages, debouncedQuery) {
        val q = debouncedQuery.lowercase()
        agencies.filter { ag ->
            ag.active && (
                q.isBlank() ||
                ag.name.lowercase().contains(q) ||
                ag.city.lowercase().contains(q) ||
                ag.tier.lowercase().contains(q) ||
                ag.description.lowercase().contains(q)
            )
        }
    }

    // 2. Matched Destinations
    val matchedDestinations = remember(destinations, debouncedQuery, selectedRegion) {
        val q = debouncedQuery.lowercase()
        destinations.filter { dest ->
            val matchRegion = if (selectedRegion == "All") true else dest.region.equals(selectedRegion, ignoreCase = true)
            val matchQuery = if (q.isBlank()) true else {
                dest.name.lowercase().contains(q) ||
                dest.state.lowercase().contains(q) ||
                dest.region.lowercase().contains(q) ||
                dest.tags.any { it.lowercase().contains(q) }
            }
            matchRegion && matchQuery
        }
    }

    // 3. Matched Packages
    val filteredPackages = remember(packages, destinations, agencies, debouncedQuery, selectedRegion, maxBudget, minDuration, selectedAgencyId, minRating, selectedHotelCategory, selectedVehicleType, selectedMealPlan, selectedCuisine, sortBy) {
        packages.filter { pkg ->
            val dest = destinations.find { it.id == pkg.destinationId }
            val ag = agencies.find { it.id == pkg.agencyId }

            val matchesSearch = if (debouncedQuery.isBlank()) true else {
                val q = debouncedQuery.lowercase()
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

    // Grouped Suggestions: Places, Agencies, Packages
    val groupedSuggestions = remember(searchQuery, destinations, agencies, packages) {
        if (searchQuery.length < 2) null
        else {
            val q = searchQuery.lowercase().trim()
            val places = destinations.filter { it.name.lowercase().contains(q) || it.state.lowercase().contains(q) }.take(3)
            val ags = agencies.filter { it.active && (it.name.lowercase().contains(q) || it.city.lowercase().contains(q)) }.take(3)
            val pkgs = packages.filter { it.title.lowercase().contains(q) }.take(3)
            Triple(places, ags, pkgs)
        }
    }

        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
        val firstName = remember(customerProfile, currentUser) {
            customerProfile?.name?.trim()?.split("\\s+".toRegex())?.firstOrNull()?.ifBlank { null }
                ?: currentUser?.displayName?.trim()?.split("\\s+".toRegex())?.firstOrNull()?.ifBlank { null }
                ?: currentUser?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
        }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Explore,
                                            contentDescription = "Tour Manage",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Tour Manage",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    if (currentUser != null && firstName != null) {
                                        Text(
                                            text = "Hi, $firstName",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            // Wishlist icon with count badge
                            IconButton(
                                onClick = onNavigateToWishlist,
                                modifier = Modifier
                                    .testTag("wishlist_button")
                                    .minimumInteractiveComponentSize()
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (wishlist.isNotEmpty()) {
                                            Badge { Text(wishlist.size.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (wishlist.isNotEmpty()) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Wishlist",
                                        tint = if (wishlist.isNotEmpty()) StatusRed else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Notification bell with unread badge
                            IconButton(
                                onClick = onNavigateToNotifications,
                                modifier = Modifier
                                    .testTag("notifications_button")
                                    .minimumInteractiveComponentSize()
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadNotifCount > 0) {
                                            Badge { Text(unreadNotifCount.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                                }
                            }

                            // Profile avatar
                            IconButton(
                                onClick = onNavigateToProfile,
                                modifier = Modifier
                                    .testTag("profile_button")
                                    .minimumInteractiveComponentSize()
                            ) {
                                if (currentUser != null) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = userInitials,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                } else {
                                    Icon(Icons.Outlined.AccountCircle, contentDescription = "Profile")
                                }
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            scrolledContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Pinned Search Bar under TopAppBar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("search_bar"),
                            placeholder = {
                                Text(
                                    text = "Search places, agencies, packages",
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { searchQuery = ""; debouncedQuery = "" },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    IconButton(
                                        onClick = { showFilterSheet = true },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("filter_button")
                                    ) {
                                        Icon(Icons.Default.Tune, contentDescription = "Filters", modifier = Modifier.size(20.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(26.dp),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                        )
                    }

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
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

                // Grouped As-You-Type Suggestions: Places | Agencies | Packages
                if (groupedSuggestions != null && (groupedSuggestions.first.isNotEmpty() || groupedSuggestions.second.isNotEmpty() || groupedSuggestions.third.isNotEmpty())) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp),
                            shadowElevation = 6.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                // 1. Places suggestions
                                if (groupedSuggestions.first.isNotEmpty()) {
                                    Text(
                                        text = "PLACES",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                    )
                                    groupedSuggestions.first.forEach { dest ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    searchQuery = dest.name
                                                    onNavigateToDestination(dest.id)
                                                }
                                                .padding(horizontal = 14.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = TourNavy)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(text = "${dest.name}, ${dest.state}", fontSize = 13.sp)
                                        }
                                    }
                                }

                                // 2. Agencies suggestions
                                if (groupedSuggestions.second.isNotEmpty()) {
                                    Text(
                                        text = "AGENCIES",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TourGoldDark,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                    )
                                    groupedSuggestions.second.forEach { ag ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    searchQuery = ag.name
                                                    onNavigateToAgency(ag.id)
                                                }
                                                .padding(horizontal = 14.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp), tint = TourGoldDark)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(text = "${ag.name} (${ag.city})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }

                                // 3. Packages suggestions
                                if (groupedSuggestions.third.isNotEmpty()) {
                                    Text(
                                        text = "PACKAGES",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TourTeal,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                    )
                                    groupedSuggestions.third.forEach { p ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    searchQuery = p.title
                                                    onNavigateToPackage(p.id)
                                                }
                                                .padding(horizontal = 14.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Luggage, contentDescription = null, modifier = Modifier.size(16.dp), tint = TourTeal)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(text = p.title, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            // Results category filter chips (All | Places | Agencies | Packages)
            if (isSearching) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Places", "Agencies", "Packages").forEach { cat ->
                            FilterChip(
                                selected = searchCategoryFilter == cat,
                                onClick = { searchCategoryFilter = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            } else {
                // Region chips when browsing normally
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        regions.forEach { region ->
                            FilterChip(
                                selected = selectedRegion == region,
                                onClick = { selectedRegion = region },
                                label = { Text(region, fontSize = 12.sp) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }
            }

            // SEARCH RESULTS VIEW
            if (isSearching) {
                val showAgencies = searchCategoryFilter == "All" || searchCategoryFilter == "Agencies"
                val showPlaces = searchCategoryFilter == "All" || searchCategoryFilter == "Places"
                val showPackages = searchCategoryFilter == "All" || searchCategoryFilter == "Packages"

                // 1. Agencies in Search
                if (showAgencies && matchedAgencies.isNotEmpty()) {
                    item {
                        Text(
                            text = "Agencies Matching '$debouncedQuery' (${matchedAgencies.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(matchedAgencies) { agency ->
                        val count = packages.count { it.agencyId == agency.id && it.active }
                        AgencyCard(
                            agency = agency,
                            packageCount = count,
                            onClick = { onNavigateToAgency(agency.id) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                // 2. Places in Search
                if (showPlaces && matchedDestinations.isNotEmpty()) {
                    item {
                        Text(
                            text = "Destinations Matching '$debouncedQuery' (${matchedDestinations.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(matchedDestinations) { dest ->
                                DestinationHeroCard(
                                    destination = dest,
                                    onClick = { onNavigateToDestination(dest.id) }
                                )
                            }
                        }
                    }
                }

                // 3. Packages in Search
                if (showPackages) {
                    item {
                        Text(
                            text = "Packages Matching '$debouncedQuery' (${filteredPackages.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
                        )
                    }
                    if (filteredPackages.isEmpty() && (!showAgencies || matchedAgencies.isEmpty()) && (!showPlaces || matchedDestinations.isEmpty())) {
                        item {
                            EmptyState(
                                icon = Icons.Outlined.SearchOff,
                                title = "No results found for '$debouncedQuery'",
                                subtitle = "Try searching for a different city, agency, state or attraction."
                            )
                        }
                    } else {
                        items(filteredPackages) { pkg ->
                            val ag = agencies.find { it.id == pkg.agencyId }
                            val dest = destinations.find { it.id == pkg.destinationId }
                            val isCompared = compareIds.contains(pkg.id)
                            val isSaved = repository.isWishlisted(pkg.id)

                            TourPackageCard(
                                pkg = pkg,
                                agency = ag,
                                destination = dest,
                                isCompared = isCompared,
                                isWishlisted = isSaved,
                                onWishlistToggle = {
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
                                onCompareToggle = { repository.toggleCompare(pkg.id) },
                                onClick = { onNavigateToPackage(pkg.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            } else {
                // NORMAL BROWSING VIEW

                // Featured Destinations Section
                if (selectedRegion == "All") {
                    item {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
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

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(destinations.filter { it.featured || it.isDemo }) { dest ->
                                    DestinationHeroCard(
                                        destination = dest,
                                        onClick = { onNavigateToDestination(dest.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Partner Agencies Horizontal Row
                item {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Partner Agencies",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${agencies.count { it.active }} Verified",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(agencies.filter { it.active }) { agency ->
                                PartnerAgencyCard(
                                    agency = agency,
                                    onClick = { onNavigateToAgency(agency.id) }
                                )
                            }
                        }
                    }
                }

                // All Tour Packages Header & List
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

                        var sortMenuOpen by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { sortMenuOpen = true }) {
                                Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
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
                            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                                DropdownMenuItem(text = { Text("Top Rated") }, onClick = { sortBy = "rating"; sortMenuOpen = false })
                                DropdownMenuItem(text = { Text("Price: Low to High") }, onClick = { sortBy = "price_asc"; sortMenuOpen = false })
                                DropdownMenuItem(text = { Text("Price: High to Low") }, onClick = { sortBy = "price_desc"; sortMenuOpen = false })
                            }
                        }
                    }
                }

                items(filteredPackages) { pkg ->
                    val ag = agencies.find { it.id == pkg.agencyId }
                    val dest = destinations.find { it.id == pkg.destinationId }
                    val isCompared = compareIds.contains(pkg.id)
                    val isSaved = wishlist.any { it.packageId == pkg.id }

                    TourPackageCard(
                        pkg = pkg,
                        agency = ag,
                        destination = dest,
                        isCompared = isCompared,
                        isWishlisted = isSaved,
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

                item {
                    Spacer(modifier = Modifier.height(30.dp))
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
}

@Composable
fun TourPackageCard(
    pkg: TourPackage,
    agency: Agency?,
    destination: Destination?,
    isCompared: Boolean,
    isWishlisted: Boolean = false,
    onWishlistToggle: (() -> Unit)? = null,
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0x55000000), Color.Transparent, Color(0xAA000000))
                            )
                        )
                )

                // Duration badge on top left
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
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Top right: ONLY the heart (wishlist) button (40 dp circular with semi-transparent dark background)
                if (onWishlistToggle != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        IconButton(
                            onClick = onWishlistToggle,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x77000000), CircleShape)
                                .minimumInteractiveComponentSize()
                                .testTag("wishlist_btn_${pkg.id}")
                        ) {
                            Icon(
                                imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isWishlisted) "Remove from Wishlist" else "Add to Wishlist",
                                tint = if (isWishlisted) StatusRed else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                RatingBadge(
                    rating = pkg.rating,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                )
            }

            // Body Content
            Column(modifier = Modifier.padding(14.dp)) {
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
                            softWrap = false,
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
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(text = pkg.vehicle.type + if (pkg.vehicle.ac) " (AC)" else "", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(text = pkg.food.mealPlan, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Icon(Icons.Default.Hotel, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(text = pkg.hotel.category, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Row with labelled Compare button and View Details button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IndianPriceText(amount = pkg.pricePerPerson)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onCompareToggle,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            colors = if (isCompared) {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = TourGold.copy(alpha = 0.15f),
                                    contentColor = TourGoldDark
                                )
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("compare_btn_${pkg.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCompared) "Added" else "Compare",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Button(
                            onClick = onClick,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("details_btn_${pkg.id}")
                        ) {
                            Text(
                                text = "View Details",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgencyLogoAvatar(
    logoUrl: String,
    agencyName: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp
) {
    val initials = remember(agencyName) {
        val parts = agencyName.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (parts.size >= 2) {
            "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
        } else {
            agencyName.take(2).uppercase().ifBlank { "AG" }
        }
    }
    val avatarBgColor = remember(agencyName) {
        val colors = listOf(
            Color(0xFF1E3A8A), Color(0xFF0F766E), Color(0xFFB45309),
            Color(0xFF4338CA), Color(0xFF047857), Color(0xFFC2410C),
            Color(0xFF6D28D9), Color(0xFF0369A1), Color(0xFFBE185D)
        )
        val idx = kotlin.math.abs(agencyName.hashCode()) % colors.size
        colors[idx]
    }

    Surface(
        shape = CircleShape,
        color = avatarBgColor,
        contentColor = Color.White,
        modifier = modifier.size(size)
    ) {
        if (logoUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = logoUrl,
                contentDescription = agencyName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            fontWeight = FontWeight.Bold,
                            fontSize = (size.value * 0.35).sp,
                            color = Color.White
                        )
                    }
                },
                error = {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            fontWeight = FontWeight.Bold,
                            fontSize = (size.value * 0.35).sp,
                            color = Color.White
                        )
                    }
                }
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initials,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.35).sp,
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PartnerAgencyCard(
    agency: Agency,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(300.dp)
            .height(130.dp)
            .clickable(onClick = onClick)
            .testTag("agency_card_${agency.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Circular 48 dp logo + Agency Name & City on Left; Rating Pill on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AgencyLogoAvatar(
                    logoUrl = agency.logoUrl,
                    agencyName = agency.name,
                    size = 48.dp
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = agency.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = agency.city,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Rating pill: compact one-line pill "★ 4.9", maxLines = 1, softWrap = false, minimum width
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TourGoldLight,
                    contentColor = TourGoldDark,
                    modifier = Modifier.widthIn(min = 52.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "★ ${String.format(java.util.Locale.US, "%.1f", agency.rating)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Bottom row: Verified and Premium chips in FlowRow so chips move to next line instead of squeezing
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (agency.verified) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TourTealLight,
                        contentColor = TourTeal
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Verified",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        text = agency.tier.replaceFirstChar { it.uppercase() },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DestinationHeroCard(
    destination: Destination,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(220.dp)
            .height(150.dp)
            .clickable(onClick = onClick)
            .testTag("destination_card_${destination.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = destination.coverImageUrl.ifBlank { "https://images.unsplash.com/photo-1595815771614-ade9d652a65d?w=800" },
                contentDescription = destination.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0x99000000), Color(0xEE000000))
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
                    color = TourGold.copy(alpha = 0.9f),
                    contentColor = Color.Black
                ) {
                    Text(
                        text = destination.region.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = destination.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${destination.state} • ${destination.bestSeason}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
