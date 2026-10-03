package com.example.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Departure
import com.example.data.models.formatInr
import com.example.data.models.formatTimestampDate
import com.example.data.repository.TourRepository
import com.example.ui.components.DepartureStatusChip
import com.example.ui.components.EmptyState
import com.example.ui.components.IndianPriceText
import com.example.ui.components.VerifiedAgencyBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    repository: TourRepository,
    onNavigateToBooking: (String, String) -> Unit
) {
    val departures by repository.departures.collectAsState()
    val packages by repository.packages.collectAsState()
    val agencies by repository.agencies.collectAsState()
    val destinations by repository.destinations.collectAsState()

    // Filters
    var selectedDestinationId by remember { mutableStateOf<String?>(null) }
    var selectedAgencyId by remember { mutableStateOf<String?>(null) }
    var selectedPackageId by remember { mutableStateOf<String?>(null) }

    // Calendar state
    val calendar = remember { Calendar.getInstance() }
    var currentYearMonth by remember {
        mutableStateOf(calendar.get(Calendar.YEAR) to calendar.get(Calendar.MONTH))
    }
    var selectedDateDay by remember {
        mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH))
    }

    // Filtered departures
    val filteredDepartures = remember(departures, selectedDestinationId, selectedAgencyId, selectedPackageId) {
        departures.filter { dep ->
            val matchDest = if (selectedDestinationId != null) dep.destinationId == selectedDestinationId else true
            val matchAgency = if (selectedAgencyId != null) dep.agencyId == selectedAgencyId else true
            val matchPackage = if (selectedPackageId != null) dep.packageId == selectedPackageId else true
            matchDest && matchAgency && matchPackage
        }
    }

    // Map of (Year, Month, Day) -> list of departures
    val departureMap = remember(filteredDepartures) {
        val map = mutableMapOf<Triple<Int, Int, Int>, MutableList<Departure>>()
        val cal = Calendar.getInstance()
        filteredDepartures.forEach { dep ->
            dep.date?.let { ts ->
                cal.time = ts.toDate()
                val key = Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                map.getOrPut(key) { mutableListOf() }.add(dep)
            }
        }
        map
    }

    val selectedDayDepartures = remember(currentYearMonth, selectedDateDay, departureMap) {
        val key = Triple(currentYearMonth.first, currentYearMonth.second, selectedDateDay)
        departureMap[key] ?: emptyList()
    }

    // Month navigation
    val currentMonthCalendar = remember(currentYearMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYearMonth.first)
            set(Calendar.MONTH, currentYearMonth.second)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val monthTitle = remember(currentYearMonth) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonthCalendar.time)
    }

    val daysInMonth = currentMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = currentMonthCalendar.get(Calendar.DAY_OF_WEEK) - 1 // 0-based for Sun

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Availability Calendar", fontWeight = FontWeight.Bold)
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
            // Filters Row (Destination, Agency, Package)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Destination Filter
                        var destMenuOpen by remember { mutableStateOf(false) }
                        val destName = destinations.find { it.id == selectedDestinationId }?.name ?: "All Places"
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { destMenuOpen = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = destName,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(expanded = destMenuOpen, onDismissRequest = { destMenuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("All Places") },
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

                        // Agency Filter
                        var agencyMenuOpen by remember { mutableStateOf(false) }
                        val agencyName = agencies.find { it.id == selectedAgencyId }?.name ?: "All Agencies"
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { agencyMenuOpen = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = agencyName,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(expanded = agencyMenuOpen, onDismissRequest = { agencyMenuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("All Agencies") },
                                    onClick = { selectedAgencyId = null; agencyMenuOpen = false }
                                )
                                agencies.forEach { a ->
                                    DropdownMenuItem(
                                        text = { Text(a.name) },
                                        onClick = { selectedAgencyId = a.id; agencyMenuOpen = false }
                                    )
                                }
                            }
                        }
                    }

                    // Status Legend Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatusLegendItem(color = StatusGreen, label = "Open")
                        StatusLegendItem(color = StatusOrange, label = "Limited")
                        StatusLegendItem(color = StatusRed, label = "Full")
                        StatusLegendItem(color = StatusGrey, label = "None")
                    }
                }
            }

            // Month View Calendar
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Month Header Navigation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                val newMonth = if (currentYearMonth.second == 0) 11 else currentYearMonth.second - 1
                                val newYear = if (currentYearMonth.second == 0) currentYearMonth.first - 1 else currentYearMonth.first
                                currentYearMonth = newYear to newMonth
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                            }

                            Text(
                                text = monthTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(onClick = {
                                val newMonth = if (currentYearMonth.second == 11) 0 else currentYearMonth.second + 1
                                val newYear = if (currentYearMonth.second == 11) currentYearMonth.first + 1 else currentYearMonth.first
                                currentYearMonth = newYear to newMonth
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Days of week row
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { dayName ->
                                Text(
                                    text = dayName,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Calendar Grid Days
                        val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7
                        var currentDay = 1

                        for (week in 0 until (totalCells / 7)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                for (dayOfWeek in 0..6) {
                                    val cellIndex = week * 7 + dayOfWeek
                                    if (cellIndex < firstDayOfWeek || currentDay > daysInMonth) {
                                        // Empty cell
                                        Box(modifier = Modifier.weight(1f))
                                    } else {
                                        val dayNumber = currentDay
                                        val dateKey = Triple(currentYearMonth.first, currentYearMonth.second, dayNumber)
                                        val dayDepartures = departureMap[dateKey] ?: emptyList()
                                        val isSelected = selectedDateDay == dayNumber

                                        // Status color logic: green open, orange limited, red full, grey none
                                        val statusColor = when {
                                            dayDepartures.isEmpty() -> StatusGrey
                                            dayDepartures.any { it.status == "open" } -> StatusGreen
                                            dayDepartures.any { it.status == "limited" } -> StatusOrange
                                            dayDepartures.all { it.status == "full" } -> StatusRed
                                            else -> StatusGrey
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(2.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                                )
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { selectedDateDay = dayNumber }
                                                .padding(2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = dayNumber.toString(),
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                // Status dot
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(statusColor)
                                                )
                                            }
                                        }
                                        currentDay++
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Departures for Selected Day
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formattedSelected = String.format("%02d %s", selectedDateDay, monthTitle)
                    Text(
                        text = "Departures on $formattedSelected (${selectedDayDepartures.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (selectedDayDepartures.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.EventBusy,
                        title = "No departures scheduled on this day",
                        subtitle = "Select green or orange dates on the calendar to see available fixed departures."
                    )
                }
            } else {
                items(selectedDayDepartures) { dep ->
                    val pkg = packages.find { it.id == dep.packageId }
                    val ag = agencies.find { it.id == dep.agencyId }
                    val price = dep.priceOverride ?: pkg?.pricePerPerson ?: 0L

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = ag?.name ?: "Agency",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (ag != null) {
                                        VerifiedAgencyBadge(verified = ag.verified, tier = ag.tier)
                                    }
                                }

                                DepartureStatusChip(status = dep.status)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = pkg?.title ?: "Tour Departure",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    IndianPriceText(amount = price, fontSize = 16)
                                    Text(
                                        text = "${dep.seatsLeft} of ${dep.seatsTotal} seats remaining",
                                        fontSize = 11.sp,
                                        color = if (dep.seatsLeft <= 4) StatusOrange else StatusGreen,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (pkg != null) {
                                            onNavigateToBooking(pkg.id, dep.id)
                                        }
                                    },
                                    enabled = dep.status == "open" || dep.status == "limited",
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("book_departure_${dep.id}")
                                ) {
                                    Text("Book Date", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun StatusLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
