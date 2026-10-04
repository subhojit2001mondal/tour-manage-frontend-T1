package com.example.ui.screens.booking

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.*
import com.example.data.repository.AuthRepository
import com.example.data.repository.TourRepository
import com.example.ui.components.BackendNotConnectedBanner
import com.example.ui.components.DepartureStatusChip
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    packageId: String,
    initialDepartureId: String,
    repository: TourRepository,
    authRepo: AuthRepository,
    onNavigateBack: () -> Unit,
    onNavigateToMyTrips: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToAuth: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val packages by repository.packages.collectAsState()
    val agencies by repository.agencies.collectAsState()
    val destinations by repository.destinations.collectAsState()
    val departures by repository.departures.collectAsState()
    val currentUser by authRepo.currentUser.collectAsState()
    val customerProfile by authRepo.customerProfile.collectAsState()

    val pkg = packages.find { it.id == packageId }
    val agency = agencies.find { it.id == pkg?.agencyId }
    val destination = destinations.find { it.id == pkg?.destinationId }
    val packageDepartures = departures.filter { it.packageId == packageId }

    // Selected departure
    var selectedDepartureId by remember {
        mutableStateOf(
            if (initialDepartureId.isNotBlank()) initialDepartureId
            else packageDepartures.firstOrNull()?.id ?: ""
        )
    }
    val selectedDeparture = departures.find { it.id == selectedDepartureId } ?: packageDepartures.firstOrNull() ?: Departure(packageId = packageId)

    // Booking stages: 0: Form, 1: Hold / Payment (15-min countdown), 2: Confirmation
    var bookingStage by remember { mutableIntStateOf(0) }
    var holdResponse by remember { mutableStateOf<BookingHoldResponse?>(null) }
    var confirmedBooking by remember { mutableStateOf<Booking?>(null) }
    var paymentErrorMessage by remember { mutableStateOf<String?>(null) }

    // Travelers list
    val travelers = remember {
        mutableStateListOf(
            Traveler(name = customerProfile?.name ?: "Primary Traveler", age = 28)
        )
    }
    var notes by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }

    // 15-minute hold timer (in seconds: 900)
    var remainingHoldSeconds by remember { mutableIntStateOf(900) }

    LaunchedEffect(bookingStage) {
        if (bookingStage == 1) {
            remainingHoldSeconds = 900
            while (remainingHoldSeconds > 0 && bookingStage == 1) {
                delay(1000L)
                remainingHoldSeconds--
            }
            if (remainingHoldSeconds <= 0 && bookingStage == 1) {
                bookingStage = 0
                Toast.makeText(context, "Reservation hold expired. Please try again.", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (pkg == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Book Tour") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { p ->
            Box(modifier = Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                Text("Tour package not found")
            }
        }
        return
    }

    val basePrice = selectedDeparture.priceOverride ?: pkg.pricePerPerson
    val subtotal = basePrice * travelers.size
    val gstAmount = (subtotal * 0.05).toLong() // 5% GST on tour package
    val totalAmount = subtotal + gstAmount

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (bookingStage) {
                            0 -> "Review & Book"
                            1 -> "Payment & Hold"
                            else -> "Booking Confirmed! 🎉"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (bookingStage != 2) {
                        IconButton(onClick = {
                            if (bookingStage == 1) bookingStage = 0 else onNavigateBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (bookingStage) {
                0 -> {
                    // STAGE 0: TRAVELER DETAILS & REVIEW
                    item {
                        // Package summary card
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = pkg.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Operated by ${agency?.name ?: "Verified Agency"} • ${pkg.days}D/${pkg.nights}N",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Departure selection
                    item {
                        Text(
                            text = "Select Departure Date",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        if (packageDepartures.isEmpty()) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(
                                    text = "Daily departure on request (Date will be confirmed by operator)",
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                packageDepartures.forEach { dep ->
                                    val isSelected = dep.id == selectedDepartureId
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                                        ),
                                        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(width = 1.5.dp) else CardDefaults.outlinedCardBorder(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedDepartureId = dep.id }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = formatTimestampDate(dep.date),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "${dep.seatsLeft} seats remaining",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            DepartureStatusChip(status = dep.status)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Travelers List & Form
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Travelers (${travelers.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = {
                                if (travelers.size < pkg.maxGroupSize) {
                                    travelers.add(Traveler(name = "", age = 25))
                                } else {
                                    Toast.makeText(context, "Maximum group size reached", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Traveler")
                            }
                        }
                    }

                    itemsIndexed(travelers) { index, traveler ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Traveler ${index + 1} ${if (index == 0) "(Primary)" else ""}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    if (index > 0) {
                                        IconButton(
                                            onClick = { travelers.removeAt(index) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = traveler.name,
                                    onValueChange = { travelers[index] = traveler.copy(name = it) },
                                    label = { Text("Full Name (as on ID proof)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = traveler.age.toString(),
                                    onValueChange = {
                                        val ageInt = it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0
                                        travelers[index] = traveler.copy(age = ageInt)
                                    },
                                    label = { Text("Age") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    // Special Notes
                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Special Requests / Dietary Notes (e.g. Jain meal)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 2
                        )
                    }

                    // Price Breakdown
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Price Summary",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${formatInr(basePrice)} × ${travelers.size} travelers",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(text = formatInr(subtotal), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Taxes & Tourism GST (5%)",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(text = formatInr(gstAmount), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Total Payable Amount",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = formatInr(totalAmount),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Terms Checkbox
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { termsAccepted = !termsAccepted }
                        ) {
                            Checkbox(
                                checked = termsAccepted,
                                onCheckedChange = { termsAccepted = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "I agree to the Tour Terms, verified agency conditions, and the cancellation policy.",
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Proceed Button
                    item {
                        Button(
                            onClick = {
                                if (currentUser == null) {
                                    Toast.makeText(context, "Please log in to hold and book your tour.", Toast.LENGTH_SHORT).show()
                                    onNavigateToAuth()
                                    return@Button
                                }
                                if (customerProfile?.name.isNullOrBlank() || customerProfile?.phone.isNullOrBlank()) {
                                    Toast.makeText(context, "Booking requires a saved name and phone number in Profile.", Toast.LENGTH_LONG).show()
                                    onNavigateToAuth()
                                    return@Button
                                }
                                if (travelers.any { it.name.isBlank() }) {
                                    Toast.makeText(context, "Please enter names for all travelers.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (!termsAccepted) {
                                    Toast.makeText(context, "Please accept the tour terms to proceed.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                scope.launch {
                                    val res = repository.holdBooking(
                                        pkg = pkg,
                                        departure = selectedDeparture,
                                        travelers = travelers,
                                        notes = notes
                                    )
                                    if (res.isSuccess) {
                                        holdResponse = res.getOrNull()
                                        bookingStage = 1
                                    } else {
                                        Toast.makeText(context, "Hold failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("continue_to_payment_button"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text("Lock Price & Hold Seats (15 Min Free)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }

                1 -> {
                    // STAGE 1: HOLD & PAYMENT
                    val hold = holdResponse
                    val minutes = remainingHoldSeconds / 60
                    val seconds = remainingHoldSeconds % 60
                    val countdownStr = String.format("%02d:%02d", minutes, seconds)

                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = TourGoldLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = TourGoldDark
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Seats Reserved: $countdownStr remaining",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = TourGoldDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Booking Reference: ${hold?.bookingCode ?: "TM-TEMP"}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF78350F)
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Order Details",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = pkg.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = "Travel Date: ${formatTimestampDate(selectedDeparture.date)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "Travelers: ${travelers.size} Persons", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Amount to Pay:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(formatInr(totalAmount), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    if (paymentErrorMessage != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = paymentErrorMessage!!,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    // Demo payment gateway simulation controls (as requested: "If the response mode is demo, show a simple Demo Payment screen with Pay successfully and Fail payment buttons that call /api/bookings/verify with demoResult. Handle success, failure and cancel.")
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Payment Gateway (Simulation Mode)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Supported methods: UPI (GPay, PhonePe, Paytm), Net Banking, Cards & Wallets",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        paymentErrorMessage = null
                                        scope.launch {
                                            val bId = hold?.bookingId ?: "bk_${System.currentTimeMillis()}"
                                            val bCode = hold?.bookingCode ?: "TM-123456"
                                            val verifyRes = repository.verifyBooking(
                                                bookingId = bId,
                                                bookingCode = bCode,
                                                pkg = pkg,
                                                departure = selectedDeparture,
                                                travelers = travelers,
                                                totalAmount = totalAmount,
                                                demoResult = true
                                            )
                                            if (verifyRes.isSuccess) {
                                                confirmedBooking = verifyRes.getOrNull()
                                                bookingStage = 2
                                            } else {
                                                paymentErrorMessage = verifyRes.exceptionOrNull()?.message
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("pay_success_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StatusGreen,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pay Successfully (${formatInr(totalAmount)})", fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val bId = hold?.bookingId ?: "bk_${System.currentTimeMillis()}"
                                            val bCode = hold?.bookingCode ?: "TM-123456"
                                            val verifyRes = repository.verifyBooking(
                                                bookingId = bId,
                                                bookingCode = bCode,
                                                pkg = pkg,
                                                departure = selectedDeparture,
                                                travelers = travelers,
                                                totalAmount = totalAmount,
                                                demoResult = false
                                            )
                                            paymentErrorMessage = "Payment simulation failed as requested. You can retry with 'Pay Successfully'."
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("fail_payment_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(imageVector = Icons.Default.Cancel, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Fail Payment (Test Error Handling)")
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                TextButton(
                                    onClick = {
                                        bookingStage = 0
                                        Toast.makeText(context, "Reservation cancelled.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cancel & Return to Details")
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // STAGE 2: CONFIRMATION SCREEN
                    val bk = confirmedBooking
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFDCFCE7),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Confirmed",
                                        tint = StatusGreen,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Booking Confirmed!",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = StatusGreen
                            )

                            Text(
                                text = "Your tour reservation is securely locked with the agency.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Prominent booking code card
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "BOOKING CODE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = bk?.bookingCode ?: holdResponse?.bookingCode ?: "TM-SUCCESS",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Trip Details
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(text = "Summary:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = pkg.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(text = "Operator: ${agency?.name ?: "Verified Agency"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "Date: ${formatTimestampDate(selectedDeparture.date)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "Travelers: ${travelers.map { it.name }.joinToString(", ")}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "Total Paid: ${formatInr(totalAmount)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = onNavigateToMyTrips,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("view_my_trips_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Luggage, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View in My Trips")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = onNavigateToChat,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Chat, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Chat About This Booking")
                            }
                        }
                    }
                }
            }
        }
    }
}
