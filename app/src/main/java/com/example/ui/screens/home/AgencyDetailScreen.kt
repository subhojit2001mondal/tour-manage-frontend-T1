package com.example.ui.screens.home

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
import com.example.data.models.formatTimestampDate
import com.example.data.repository.TourRepository
import com.example.ui.components.*
import com.example.ui.theme.TourGold
import com.example.ui.theme.TourNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgencyDetailScreen(
    agencyId: String,
    repository: TourRepository,
    onNavigateBack: () -> Unit,
    onNavigateToPackage: (String) -> Unit,
    onNavigateToBooking: (String, String) -> Unit,
    onNavigateToSupport: () -> Unit
) {
    val agencies by repository.agencies.collectAsState()
    val packages by repository.packages.collectAsState()
    val departures by repository.departures.collectAsState()
    val destinations by repository.destinations.collectAsState()
    val compareIds by repository.comparePackageIds.collectAsState()

    val agency = agencies.find { it.id == agencyId }
    val agencyPackages = packages.filter { it.agencyId == agencyId && it.active }
    val agencyDepartures = departures.filter { it.agencyId == agencyId }

    if (agency == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Agency Details") },
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
                Text("Agency not found")
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(agency.name) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Agency Header Profile
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(64.dp)
                            ) {
                                if (agency.logoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = agency.logoUrl,
                                        contentDescription = agency.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Business,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = agency.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    VerifiedAgencyBadge(verified = agency.verified, tier = agency.tier)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    RatingBadge(rating = agency.rating)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${agency.city} • Verified Local Partner",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = agency.description.ifBlank { "Professional registered local tour agency bringing specialized regional itineraries, private transport, and boutique hospitality under the Tour Manage marketplace." },
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        // Note about customer contact through Support tab
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Bookings and communications with this agency are protected by Tour Manage. Questions? Tap to message support.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onNavigateToSupport() }
                                )
                            }
                        }
                    }
                }
            }

            // Next Departures from this Agency
            if (agencyDepartures.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            text = "Next Available Departures",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(agencyDepartures) { dep ->
                                val pkg = packages.find { it.id == dep.packageId }
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = CardDefaults.outlinedCardBorder(),
                                    modifier = Modifier.clickable {
                                        if (pkg != null) onNavigateToBooking(pkg.id, dep.id)
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

            // Packages offered by this agency
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tours by ${agency.name} (${agencyPackages.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (agencyPackages.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Luggage,
                        title = "No active packages currently listed",
                        subtitle = "Check back soon as this agency posts new seasonal itineraries."
                    )
                }
            } else {
                items(agencyPackages) { pkg ->
                    val dest = destinations.find { it.id == pkg.destinationId }
                    val isCompared = compareIds.contains(pkg.id)

                    TourPackageCard(
                        pkg = pkg,
                        agency = agency,
                        destination = dest,
                        isCompared = isCompared,
                        onCompareToggle = { repository.toggleCompare(pkg.id) },
                        onClick = { onNavigateToPackage(pkg.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
