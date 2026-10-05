package com.example.ui.screens.compare

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.*
import com.example.data.repository.TourRepository
import com.example.ui.components.EmptyState
import com.example.ui.components.RatingBadge
import com.example.ui.components.VerifiedAgencyBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    repository: TourRepository,
    onNavigateToBooking: (String, String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToDestination: (String) -> Unit = {},
    onNavigateToPackage: (String) -> Unit = {}
) {
    val destinations by repository.destinations.collectAsState()
    val agencies by repository.agencies.collectAsState()
    val packages by repository.packages.collectAsState()
    val departures by repository.departures.collectAsState()
    val compareIds by repository.comparePackageIds.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Agencies, 1: Destinations, 2: My list

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
                                    imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Comparison Center",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = when (selectedTab) {
                                    0 -> "Compare Agencies for Same Destination"
                                    1 -> "Compare Two Destinations"
                                    else -> "Custom Package Shortlist (${compareIds.size}/4)"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (selectedTab == 2 && compareIds.isNotEmpty()) {
                        TextButton(
                            onClick = { repository.clearCompare() },
                            modifier = Modifier.testTag("clear_compare_button")
                        ) {
                            Text("Clear All")
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
            // Three Tabs: Agencies | Destinations | My list
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Agencies", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_compare_agencies")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Destinations", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_compare_destinations")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("My list", fontWeight = FontWeight.SemiBold)
                            if (compareIds.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ) {
                                    Text(compareIds.size.toString(), fontSize = 10.sp)
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("tab_compare_my_list")
                )
            }

            when (selectedTab) {
                0 -> CompareAgenciesTab(
                    destinations = destinations,
                    agencies = agencies,
                    packages = packages,
                    departures = departures,
                    onNavigateToBooking = onNavigateToBooking
                )
                1 -> CompareDestinationsTab(
                    destinations = destinations,
                    agencies = agencies,
                    packages = packages,
                    departures = departures,
                    onNavigateToDestination = onNavigateToDestination
                )
                2 -> CompareMyListTab(
                    compareIds = compareIds,
                    packages = packages,
                    agencies = agencies,
                    departures = departures,
                    repository = repository,
                    onNavigateToBooking = onNavigateToBooking,
                    onNavigateToHome = onNavigateToHome
                )
            }
        }
    }
}

/* ==========================================================================
   TAB 1: AGENCIES COMPARISON (Same destination, two agencies)
   ========================================================================== */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareAgenciesTab(
    destinations: List<Destination>,
    agencies: List<Agency>,
    packages: List<TourPackage>,
    departures: List<Departure>,
    onNavigateToBooking: (String, String) -> Unit
) {
    if (destinations.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.TravelExplore,
            title = "Loading data...",
            subtitle = "Fetching available destinations and tour operators."
        )
        return
    }

    var selectedDestId by remember(destinations) {
        // Pick a destination that has packages from at least one agency
        val firstWithPkgs = destinations.find { d -> packages.any { it.destinationId == d.id } }
        mutableStateOf(firstWithPkgs?.id ?: destinations.firstOrNull()?.id ?: "")
    }

    val packagesForDest = remember(selectedDestId, packages) {
        packages.filter { it.destinationId == selectedDestId }
    }

    val availableAgencies = remember(packagesForDest, agencies) {
        val agencyIds = packagesForDest.map { it.agencyId }.toSet()
        agencies.filter { it.active && agencyIds.contains(it.id) }
    }

    var agencyAId by remember(availableAgencies) {
        mutableStateOf(availableAgencies.getOrNull(0)?.id ?: "")
    }
    var agencyBId by remember(availableAgencies) {
        mutableStateOf(availableAgencies.getOrNull(1)?.id ?: availableAgencies.getOrNull(0)?.id ?: "")
    }

    // Keep agency IDs valid if destination changes
    LaunchedEffect(availableAgencies) {
        if (availableAgencies.isNotEmpty()) {
            if (availableAgencies.none { it.id == agencyAId }) {
                agencyAId = availableAgencies[0].id
            }
            if (availableAgencies.none { it.id == agencyBId }) {
                agencyBId = availableAgencies.getOrNull(1)?.id ?: availableAgencies[0].id
            }
        }
    }

    val agencyAPackages = remember(agencyAId, packagesForDest) {
        packagesForDest.filter { it.agencyId == agencyAId }
    }
    val agencyBPackages = remember(agencyBId, packagesForDest) {
        packagesForDest.filter { it.agencyId == agencyBId }
    }

    var selectedPkgAId by remember(agencyAPackages) {
        mutableStateOf(agencyAPackages.minByOrNull { it.pricePerPerson }?.id ?: "")
    }
    var selectedPkgBId by remember(agencyBPackages) {
        mutableStateOf(agencyBPackages.minByOrNull { it.pricePerPerson }?.id ?: "")
    }

    LaunchedEffect(agencyAPackages) {
        if (agencyAPackages.none { it.id == selectedPkgAId }) {
            selectedPkgAId = agencyAPackages.minByOrNull { it.pricePerPerson }?.id ?: ""
        }
    }
    LaunchedEffect(agencyBPackages) {
        if (agencyBPackages.none { it.id == selectedPkgBId }) {
            selectedPkgBId = agencyBPackages.minByOrNull { it.pricePerPerson }?.id ?: ""
        }
    }

    var travelersCount by remember { mutableIntStateOf(2) }

    val agencyA = agencies.find { it.id == agencyAId }
    val agencyB = agencies.find { it.id == agencyBId }
    val pkgA = packages.find { it.id == selectedPkgAId }
    val pkgB = packages.find { it.id == selectedPkgBId }

    val nextDepA = remember(selectedPkgAId, departures) {
        departures.filter { it.packageId == selectedPkgAId && it.status != "closed" && it.status != "full" }
            .minByOrNull { it.date?.seconds ?: Long.MAX_VALUE }
    }
    val nextDepB = remember(selectedPkgBId, departures) {
        departures.filter { it.packageId == selectedPkgBId && it.status != "closed" && it.status != "full" }
            .minByOrNull { it.date?.seconds ?: Long.MAX_VALUE }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // 1. Destination Picker Dropdown
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "1. Select Destination",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    var destExpanded by remember { mutableStateOf(false) }
                    val currentDestName = destinations.find { it.id == selectedDestId }?.name ?: "Choose Destination"

                    ExposedDropdownMenuBox(
                        expanded = destExpanded,
                        onExpandedChange = { destExpanded = !destExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentDestName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = destExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("compare_destination_selector"),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = destExpanded,
                            onDismissRequest = { destExpanded = false }
                        ) {
                            destinations.forEach { dest ->
                                val count = packages.count { it.destinationId == dest.id }
                                DropdownMenuItem(
                                    text = { Text("${dest.name}, ${dest.state} ($count tours)") },
                                    onClick = {
                                        selectedDestId = dest.id
                                        destExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Check if there are active agencies for this destination
        if (availableAgencies.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.Business,
                    title = "No active operators for this destination",
                    subtitle = "Please select another destination from the dropdown above to compare partner agencies."
                )
            }
            return@LazyColumn
        }

        // 2. Agency A and Agency B Selectors + Swap button
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Select Two Operators",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = {
                                val temp = agencyAId
                                agencyAId = agencyBId
                                agencyBId = temp
                            },
                            modifier = Modifier.testTag("swap_agencies_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Swap operators",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Agency A Picker
                        var aExp by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { aExp = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "A: ${agencyA?.name ?: "Select"}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 12.sp
                                )
                            }
                            DropdownMenu(expanded = aExp, onDismissRequest = { aExp = false }) {
                                availableAgencies.forEach { ag ->
                                    DropdownMenuItem(
                                        text = { Text(ag.name) },
                                        onClick = {
                                            agencyAId = ag.id
                                            aExp = false
                                        }
                                    )
                                }
                            }
                        }

                        // Agency B Picker
                        var bExp by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { bExp = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "B: ${agencyB?.name ?: "Select"}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 12.sp
                                )
                            }
                            DropdownMenu(expanded = bExp, onDismissRequest = { bExp = false }) {
                                availableAgencies.forEach { ag ->
                                    DropdownMenuItem(
                                        text = { Text(ag.name) },
                                        onClick = {
                                            agencyBId = ag.id
                                            bExp = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Package pickers for A and B
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Package A Dropdown
                        var pkgAExp by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedCard(
                                onClick = { pkgAExp = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Package A", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = pkgA?.title ?: "No Package",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatInr(pkgA?.pricePerPerson ?: 0L) + " /person",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            DropdownMenu(expanded = pkgAExp, onDismissRequest = { pkgAExp = false }) {
                                agencyAPackages.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text("${p.title} (${formatInr(p.pricePerPerson)})") },
                                        onClick = {
                                            selectedPkgAId = p.id
                                            pkgAExp = false
                                        }
                                    )
                                }
                            }
                        }

                        // Package B Dropdown
                        var pkgBExp by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedCard(
                                onClick = { pkgBExp = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Package B", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = pkgB?.title ?: "No Package",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatInr(pkgB?.pricePerPerson ?: 0L) + " /person",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            DropdownMenu(expanded = pkgBExp, onDismissRequest = { pkgBExp = false }) {
                                agencyBPackages.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text("${p.title} (${formatInr(p.pricePerPerson)})") },
                                        onClick = {
                                            selectedPkgBId = p.id
                                            pkgBExp = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Travelers Stepper
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Number of Travelers", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Total package prices update dynamically", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (travelersCount > 1) travelersCount-- },
                            enabled = travelersCount > 1,
                            modifier = Modifier.testTag("travelers_decrement")
                        ) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                        }

                        Text(
                            text = travelersCount.toString(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        IconButton(
                            onClick = { if (travelersCount < 20) travelersCount++ },
                            enabled = travelersCount < 20,
                            modifier = Modifier.testTag("travelers_increment")
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
                        }
                    }
                }
            }
        }

        // 4. Comparison Summary Card
        if (pkgA != null && pkgB != null && agencyA != null && agencyB != null) {
            val totalA = pkgA.pricePerPerson * travelersCount
            val totalB = pkgB.pricePerPerson * travelersCount
            val priceDiff = kotlin.math.abs(totalA - totalB)
            val cheaperAgencyName = if (totalA < totalB) agencyA.name else if (totalB < totalA) agencyB.name else null

            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                    modifier = Modifier.fillMaxWidth().testTag("comparison_summary_card")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Comparison Summary",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF14532D)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val summaryText = buildString {
                                if (cheaperAgencyName != null) {
                                    append("$cheaperAgencyName is cheaper by ${formatInr(priceDiff)} for $travelersCount travelers. ")
                                } else {
                                    append("Both agencies offer identical total pricing of ${formatInr(totalA)}. ")
                                }
                                if (agencyA.rating > agencyB.rating) {
                                    append("${agencyA.name} holds the higher operator rating (★${agencyA.rating} vs ★${agencyB.rating}). ")
                                } else if (agencyB.rating > agencyA.rating) {
                                    append("${agencyB.name} holds the higher operator rating (★${agencyB.rating} vs ★${agencyA.rating}). ")
                                }
                                if (pkgA.inclusions.size > pkgB.inclusions.size) {
                                    append("${pkgA.title} provides more inclusions (${pkgA.inclusions.size} items).")
                                } else if (pkgB.inclusions.size > pkgA.inclusions.size) {
                                    append("${pkgB.title} provides more inclusions (${pkgB.inclusions.size} items).")
                                }
                            }
                            Text(
                                text = summaryText,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = Color(0xFF166534)
                            )
                        }
                    }
                }
            }

            // 5. Side-by-Side Comparison Table with Sticky/Fixed Row Labels
            item {
                Text(
                    text = "Side-by-Side Package Specifications",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                AgencyComparisonTable(
                    agencyA = agencyA,
                    agencyB = agencyB,
                    pkgA = pkgA,
                    pkgB = pkgB,
                    travelersCount = travelersCount,
                    nextDepA = nextDepA,
                    nextDepB = nextDepB,
                    onBookA = { onNavigateToBooking(pkgA.id, nextDepA?.id ?: "") },
                    onBookB = { onNavigateToBooking(pkgB.id, nextDepB?.id ?: "") }
                )
            }
        }
    }
}

@Composable
fun AgencyComparisonTable(
    agencyA: Agency,
    agencyB: Agency,
    pkgA: TourPackage,
    pkgB: TourPackage,
    travelersCount: Int,
    nextDepA: Departure?,
    nextDepB: Departure?,
    onBookA: () -> Unit,
    onBookB: () -> Unit
) {
    val totalA = pkgA.pricePerPerson * travelersCount
    val totalB = pkgB.pricePerPerson * travelersCount

    val scrollState = rememberScrollState()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Table Header Row: Sticky Label Column (100dp) + Agency A (150dp) + Agency B (150dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(8.dp)
                    .horizontalScroll(scrollState)
            ) {
                Text(
                    text = "SPECIFICATION",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(110.dp)
                )

                // Column A Header
                Column(modifier = Modifier.width(160.dp).padding(horizontal = 4.dp)) {
                    Text(agencyA.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Operator A", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                }

                // Column B Header
                Column(modifier = Modifier.width(160.dp).padding(horizontal = 4.dp)) {
                    Text(agencyB.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Operator B", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            HorizontalDivider()

            val rows = listOf(
                ComparisonRowData(
                    label = "Operator Rating",
                    valA = "★ ${agencyA.rating} (${agencyA.tier.replaceFirstChar { it.uppercase() }})",
                    valB = "★ ${agencyB.rating} (${agencyB.tier.replaceFirstChar { it.uppercase() }})",
                    bestA = agencyA.rating > agencyB.rating,
                    bestB = agencyB.rating > agencyA.rating
                ),
                ComparisonRowData(
                    label = "City & Verification",
                    valA = "${agencyA.city} ${if (agencyA.verified) "✓ Verified" else ""}",
                    valB = "${agencyB.city} ${if (agencyB.verified) "✓ Verified" else ""}",
                    bestA = agencyA.verified && !agencyB.verified,
                    bestB = agencyB.verified && !agencyA.verified
                ),
                ComparisonRowData(
                    label = "Package Title",
                    valA = pkgA.title,
                    valB = pkgB.title
                ),
                ComparisonRowData(
                    label = "Price / Person",
                    valA = formatInr(pkgA.pricePerPerson),
                    valB = formatInr(pkgB.pricePerPerson),
                    bestA = pkgA.pricePerPerson < pkgB.pricePerPerson,
                    bestB = pkgB.pricePerPerson < pkgA.pricePerPerson
                ),
                ComparisonRowData(
                    label = "Total ($travelersCount travelers)",
                    valA = formatInr(totalA),
                    valB = formatInr(totalB),
                    bestA = totalA < totalB,
                    bestB = totalB < totalA
                ),
                ComparisonRowData(
                    label = "Duration",
                    valA = "${pkgA.days}D / ${pkgA.nights}N",
                    valB = "${pkgB.days}D / ${pkgB.nights}N"
                ),
                ComparisonRowData(
                    label = "Vehicle & AC",
                    valA = "${pkgA.vehicle.type} (${pkgA.vehicle.vehicleName})\n${if (pkgA.vehicle.ac) "AC Available" else "Non-AC"}",
                    valB = "${pkgB.vehicle.type} (${pkgB.vehicle.vehicleName})\n${if (pkgB.vehicle.ac) "AC Available" else "Non-AC"}"
                ),
                ComparisonRowData(
                    label = "Meal Plan & Diet",
                    valA = "${pkgA.food.mealPlan}\n${pkgA.food.cuisine}",
                    valB = "${pkgB.food.mealPlan}\n${pkgB.food.cuisine}"
                ),
                ComparisonRowData(
                    label = "Hotel & Room",
                    valA = "${pkgA.hotel.hotelName}\n${pkgA.hotel.category} • ${pkgA.hotel.roomType}",
                    valB = "${pkgB.hotel.hotelName}\n${pkgB.hotel.category} • ${pkgB.hotel.roomType}"
                ),
                ComparisonRowData(
                    label = "Inclusions",
                    valA = "${pkgA.inclusions.size} included:\n" + pkgA.inclusions.take(3).joinToString("\n• ", prefix = "• "),
                    valB = "${pkgB.inclusions.size} included:\n" + pkgB.inclusions.take(3).joinToString("\n• ", prefix = "• "),
                    bestA = pkgA.inclusions.size > pkgB.inclusions.size,
                    bestB = pkgB.inclusions.size > pkgA.inclusions.size
                ),
                ComparisonRowData(
                    label = "Cancellation",
                    valA = pkgA.cancellationPolicy.ifBlank { "Standard 48hr refund" },
                    valB = pkgB.cancellationPolicy.ifBlank { "Standard 48hr refund" }
                ),
                ComparisonRowData(
                    label = "Next Departure",
                    valA = if (nextDepA?.date != null) "${formatTimestampDate(nextDepA.date)} (${nextDepA.seatsLeft} seats left)" else "Daily on request",
                    valB = if (nextDepB?.date != null) "${formatTimestampDate(nextDepB.date)} (${nextDepB.seatsLeft} seats left)" else "Daily on request",
                    bestA = (nextDepA?.seatsLeft ?: 0) > (nextDepB?.seatsLeft ?: 0),
                    bestB = (nextDepB?.seatsLeft ?: 0) > (nextDepA?.seatsLeft ?: 0)
                )
            )

            // Render Rows
            rows.forEachIndexed { idx, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (idx % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                        .horizontalScroll(scrollState),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = row.label,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(110.dp)
                    )

                    // Cell A
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (row.bestA) Color(0xFFDCFCE7) else Color.Transparent,
                        border = if (row.bestA) BorderStroke(1.dp, Color(0xFF86EFAC)) else null,
                        modifier = Modifier.width(160.dp).padding(horizontal = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            if (row.bestA) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF16A34A),
                                    contentColor = Color.White
                                ) {
                                    Text("BEST", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Text(
                                text = row.valA,
                                fontSize = 12.sp,
                                color = if (row.bestA) Color(0xFF14532D) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Cell B
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (row.bestB) Color(0xFFDCFCE7) else Color.Transparent,
                        border = if (row.bestB) BorderStroke(1.dp, Color(0xFF86EFAC)) else null,
                        modifier = Modifier.width(160.dp).padding(horizontal = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            if (row.bestB) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF16A34A),
                                    contentColor = Color.White
                                ) {
                                    Text("BEST", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Text(
                                text = row.valB,
                                fontSize = 12.sp,
                                color = if (row.bestB) Color(0xFF14532D) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            }

            // Booking Row under each column
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(110.dp))

                Box(modifier = Modifier.width(160.dp).padding(horizontal = 4.dp)) {
                    Button(
                        onClick = onBookA,
                        modifier = Modifier.fillMaxWidth().testTag("book_package_a_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Book A", fontSize = 12.sp)
                    }
                }

                Box(modifier = Modifier.width(160.dp).padding(horizontal = 4.dp)) {
                    Button(
                        onClick = onBookB,
                        modifier = Modifier.fillMaxWidth().testTag("book_package_b_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Book B", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

data class ComparisonRowData(
    val label: String,
    val valA: String,
    val valB: String,
    val bestA: Boolean = false,
    val bestB: Boolean = false
)

/* ==========================================================================
   TAB 2: DESTINATIONS COMPARISON (Two different places)
   ========================================================================== */
@Composable
fun CompareDestinationsTab(
    destinations: List<Destination>,
    agencies: List<Agency>,
    packages: List<TourPackage>,
    departures: List<Departure>,
    onNavigateToDestination: (String) -> Unit
) {
    if (destinations.size < 2) {
        EmptyState(
            icon = Icons.AutoMirrored.Filled.CompareArrows,
            title = "Need at least two destinations",
            subtitle = "Explore our destination catalog to enable two-way destination comparison."
        )
        return
    }

    var destAId by remember { mutableStateOf(destinations.getOrNull(0)?.id ?: "") }
    var destBId by remember { mutableStateOf(destinations.getOrNull(1)?.id ?: "") }

    val destA = destinations.find { it.id == destAId }
    val destB = destinations.find { it.id == destBId }

    // Metrics for Dest A
    val pkgsA = remember(destAId, packages) { packages.filter { it.destinationId == destAId } }
    val agenciesA = remember(pkgsA, agencies) {
        val ids = pkgsA.map { it.agencyId }.toSet()
        agencies.filter { ids.contains(it.id) }
    }
    val lowestA = pkgsA.minOfOrNull { it.pricePerPerson } ?: 0L
    val avgA = if (pkgsA.isNotEmpty()) (pkgsA.sumOf { it.pricePerPerson } / pkgsA.size) else 0L
    val minDaysA = pkgsA.minOfOrNull { it.days } ?: 0
    val maxDaysA = pkgsA.maxOfOrNull { it.days } ?: 0
    val topPkgA = pkgsA.maxByOrNull { it.rating }
    val depsA = remember(destAId, departures) { departures.filter { it.destinationId == destAId && it.status != "closed" } }
    val seatsA = depsA.sumOf { it.seatsLeft }

    // Metrics for Dest B
    val pkgsB = remember(destBId, packages) { packages.filter { it.destinationId == destBId } }
    val agenciesB = remember(pkgsB, agencies) {
        val ids = pkgsB.map { it.agencyId }.toSet()
        agencies.filter { ids.contains(it.id) }
    }
    val lowestB = pkgsB.minOfOrNull { it.pricePerPerson } ?: 0L
    val avgB = if (pkgsB.isNotEmpty()) (pkgsB.sumOf { it.pricePerPerson } / pkgsB.size) else 0L
    val minDaysB = pkgsB.minOfOrNull { it.days } ?: 0
    val maxDaysB = pkgsB.maxOfOrNull { it.days } ?: 0
    val topPkgB = pkgsB.maxByOrNull { it.rating }
    val depsB = remember(destBId, departures) { departures.filter { it.destinationId == destBId && it.status != "closed" } }
    val seatsB = depsB.sumOf { it.seatsLeft }

    val scrollState = rememberScrollState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Destination Selectors
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Pick Two Destinations to Compare",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Dropdown A
                        var aExp by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { aExp = true },
                                modifier = Modifier.fillMaxWidth().testTag("select_dest_a_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "A: ${destA?.name ?: "Select"}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 12.sp
                                )
                            }
                            DropdownMenu(expanded = aExp, onDismissRequest = { aExp = false }) {
                                destinations.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text("${d.name}, ${d.state}") },
                                        onClick = {
                                            destAId = d.id
                                            aExp = false
                                        }
                                    )
                                }
                            }
                        }

                        // Dropdown B
                        var bExp by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { bExp = true },
                                modifier = Modifier.fillMaxWidth().testTag("select_dest_b_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "B: ${destB?.name ?: "Select"}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 12.sp
                                )
                            }
                            DropdownMenu(expanded = bExp, onDismissRequest = { bExp = false }) {
                                destinations.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text("${d.name}, ${d.state}") },
                                        onClick = {
                                            destBId = d.id
                                            bExp = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (destA != null && destB != null) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .padding(8.dp)
                                .horizontalScroll(scrollState)
                        ) {
                            Text(
                                text = "CRITERIA",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(110.dp)
                            )

                            Column(modifier = Modifier.width(160.dp).padding(horizontal = 4.dp)) {
                                Text(destA.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(destA.state, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }

                            Column(modifier = Modifier.width(160.dp).padding(horizontal = 4.dp)) {
                                Text(destB.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(destB.state, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        HorizontalDivider()

                        val dRows = listOf(
                            ComparisonRowData(
                                label = "Region & State",
                                valA = "${destA.region} Region\n${destA.state}",
                                valB = "${destB.region} Region\n${destB.state}"
                            ),
                            ComparisonRowData(
                                label = "Best Season",
                                valA = destA.bestSeason,
                                valB = destB.bestSeason
                            ),
                            ComparisonRowData(
                                label = "Themes / Tags",
                                valA = destA.tags.joinToString(", ").ifBlank { "Scenic, Culture" },
                                valB = destB.tags.joinToString(", ").ifBlank { "Scenic, Culture" }
                            ),
                            ComparisonRowData(
                                label = "Operators & Tours",
                                valA = "${agenciesA.size} agencies\n${pkgsA.size} tour packages",
                                valB = "${agenciesB.size} agencies\n${pkgsB.size} tour packages",
                                bestA = pkgsA.size > pkgsB.size,
                                bestB = pkgsB.size > pkgsA.size
                            ),
                            ComparisonRowData(
                                label = "Starting Price",
                                valA = formatInr(lowestA),
                                valB = formatInr(lowestB),
                                bestA = lowestA > 0 && (lowestB == 0L || lowestA < lowestB),
                                bestB = lowestB > 0 && (lowestA == 0L || lowestB < lowestA)
                            ),
                            ComparisonRowData(
                                label = "Average Price",
                                valA = formatInr(avgA),
                                valB = formatInr(avgB),
                                bestA = avgA > 0 && (avgB == 0L || avgA < avgB),
                                bestB = avgB > 0 && (avgA == 0L || avgB < avgA)
                            ),
                            ComparisonRowData(
                                label = "Typical Duration",
                                valA = if (minDaysA > 0) "$minDaysA - $maxDaysA Days" else "Varies",
                                valB = if (minDaysB > 0) "$minDaysB - $maxDaysB Days" else "Varies"
                            ),
                            ComparisonRowData(
                                label = "Common Hotel",
                                valA = pkgsA.groupBy { it.hotel.category }.maxByOrNull { it.value.size }?.key ?: "3-star / 4-star",
                                valB = pkgsB.groupBy { it.hotel.category }.maxByOrNull { it.value.size }?.key ?: "3-star / 4-star"
                            ),
                            ComparisonRowData(
                                label = "Top-Rated Tour",
                                valA = topPkgA?.let { "★ ${it.rating}\n${it.title}" } ?: "N/A",
                                valB = topPkgB?.let { "★ ${it.rating}\n${it.title}" } ?: "N/A",
                                bestA = (topPkgA?.rating ?: 0.0) > (topPkgB?.rating ?: 0.0),
                                bestB = (topPkgB?.rating ?: 0.0) > (topPkgA?.rating ?: 0.0)
                            ),
                            ComparisonRowData(
                                label = "Upcoming Departures",
                                valA = "${depsA.size} departures\n$seatsA seats open",
                                valB = "${depsB.size} departures\n$seatsB seats open",
                                bestA = seatsA > seatsB,
                                bestB = seatsB > seatsA
                            )
                        )

                        dRows.forEachIndexed { idx, row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (idx % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                    .padding(vertical = 8.dp, horizontal = 8.dp)
                                    .horizontalScroll(scrollState),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = row.label,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(110.dp)
                                )

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (row.bestA) Color(0xFFDCFCE7) else Color.Transparent,
                                    border = if (row.bestA) BorderStroke(1.dp, Color(0xFF86EFAC)) else null,
                                    modifier = Modifier.width(160.dp).padding(horizontal = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(4.dp)) {
                                        if (row.bestA) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF16A34A),
                                                contentColor = Color.White
                                            ) {
                                                Text("BEST", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                        }
                                        Text(text = row.valA, fontSize = 12.sp, color = if (row.bestA) Color(0xFF14532D) else MaterialTheme.colorScheme.onSurface)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (row.bestB) Color(0xFFDCFCE7) else Color.Transparent,
                                    border = if (row.bestB) BorderStroke(1.dp, Color(0xFF86EFAC)) else null,
                                    modifier = Modifier.width(160.dp).padding(horizontal = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(4.dp)) {
                                        if (row.bestB) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF16A34A),
                                                contentColor = Color.White
                                            ) {
                                                Text("BEST", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                        }
                                        Text(text = row.valB, fontSize = 12.sp, color = if (row.bestB) Color(0xFF14532D) else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        }

                        // View Packages Buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .horizontalScroll(scrollState),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.width(110.dp))

                            Box(modifier = Modifier.width(160.dp).padding(horizontal = 4.dp)) {
                                OutlinedButton(
                                    onClick = { onNavigateToDestination(destA.id) },
                                    modifier = Modifier.fillMaxWidth().testTag("view_dest_a_packages"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("View ${destA.name}", fontSize = 11.sp)
                                }
                            }

                            Box(modifier = Modifier.width(160.dp).padding(horizontal = 4.dp)) {
                                OutlinedButton(
                                    onClick = { onNavigateToDestination(destB.id) },
                                    modifier = Modifier.fillMaxWidth().testTag("view_dest_b_packages"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("View ${destB.name}", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ==========================================================================
   TAB 3: MY LIST COMPARISON (The 2 to 4 packages shortlist)
   ========================================================================== */
@Composable
fun CompareMyListTab(
    compareIds: Set<String>,
    packages: List<TourPackage>,
    agencies: List<Agency>,
    departures: List<Departure>,
    repository: TourRepository,
    onNavigateToBooking: (String, String) -> Unit,
    onNavigateToHome: () -> Unit
) {
    val comparedPackages = remember(compareIds, packages) {
        packages.filter { compareIds.contains(it.id) }
    }

    if (comparedPackages.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Tours in Your Comparison List",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap 'Compare' on any tour package card on the Home screen to add up to 4 packages for a side-by-side spec comparison.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onNavigateToHome,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Explore Tours to Compare")
                }
            }
        }
        return
    }

    val lowestPrice = remember(comparedPackages) {
        comparedPackages.minOfOrNull { it.pricePerPerson } ?: 0L
    }
    val bestRating = remember(comparedPackages) {
        comparedPackages.maxOfOrNull { it.rating } ?: 0.0
    }

    val scrollState = rememberScrollState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Comparing ${comparedPackages.size} Selected Tours",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(8.dp)
                            .horizontalScroll(scrollState)
                    ) {
                        Text(
                            text = "TOUR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(100.dp)
                        )

                        comparedPackages.forEach { pkg ->
                            val agency = agencies.find { it.id == pkg.agencyId }
                            Column(modifier = Modifier.width(150.dp).padding(horizontal = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = pkg.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { repository.toggleCompare(pkg.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text(agency?.name ?: "Operator", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    HorizontalDivider()

                    // Specs Rows
                    val rows = listOf(
                        "Price / Person" to comparedPackages.map { formatInr(it.pricePerPerson) },
                        "Rating" to comparedPackages.map { "★ ${it.rating}" },
                        "Duration" to comparedPackages.map { "${it.days}D / ${it.nights}N" },
                        "Vehicle" to comparedPackages.map { "${it.vehicle.type}\n${if (it.vehicle.ac) "AC" else "Non-AC"}" },
                        "Meal Plan" to comparedPackages.map { "${it.food.mealPlan}\n${it.food.cuisine}" },
                        "Hotel" to comparedPackages.map { "${it.hotel.category}\n${it.hotel.roomType}" },
                        "Inclusions" to comparedPackages.map { "${it.inclusions.size} items" }
                    )

                    rows.forEachIndexed { idx, (label, values) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (idx % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                .padding(vertical = 8.dp, horizontal = 8.dp)
                                .horizontalScroll(scrollState),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(100.dp)
                            )

                            values.forEachIndexed { vIdx, value ->
                                val pkg = comparedPackages[vIdx]
                                val isBestPrice = label == "Price / Person" && pkg.pricePerPerson == lowestPrice && lowestPrice > 0
                                val isBestRating = label == "Rating" && pkg.rating == bestRating && bestRating > 0.0
                                val highlight = isBestPrice || isBestRating

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (highlight) Color(0xFFDCFCE7) else Color.Transparent,
                                    border = if (highlight) BorderStroke(1.dp, Color(0xFF86EFAC)) else null,
                                    modifier = Modifier.width(150.dp).padding(horizontal = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(4.dp)) {
                                        if (highlight) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF16A34A),
                                                contentColor = Color.White
                                            ) {
                                                Text("BEST", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                        }
                                        Text(text = value, fontSize = 12.sp, color = if (highlight) Color(0xFF14532D) else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    }

                    // Book Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .horizontalScroll(scrollState),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(100.dp))

                        comparedPackages.forEach { pkg ->
                            val nextDep = departures.find { it.packageId == pkg.id && it.status != "closed" }
                            Box(modifier = Modifier.width(150.dp).padding(horizontal = 4.dp)) {
                                Button(
                                    onClick = { onNavigateToBooking(pkg.id, nextDep?.id ?: "") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Book", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
