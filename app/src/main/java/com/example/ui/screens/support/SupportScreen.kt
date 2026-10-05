package com.example.ui.screens.support

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.API_BASE_URL
import com.example.data.models.ChatMessage
import com.example.data.models.formatTimestampDateTime
import com.example.data.repository.AuthRepository
import com.example.data.repository.TourRepository
import com.example.ui.components.BackendNotConnectedBanner
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    repository: TourRepository,
    authRepo: AuthRepository,
    onNavigateToAuth: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val companySettings by repository.companySettings.collectAsState()
    val chatMessages by repository.chatMessages.collectAsState()
    val chatMode by repository.chatMode.collectAsState()
    val currentUser by authRepo.currentUser.collectAsState()
    val customerProfile by authRepo.customerProfile.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Live Chat, 1: Contact & Callback
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Callback form state
    var callbackName by remember(customerProfile) { mutableStateOf(customerProfile?.name ?: "") }
    var callbackPhone by remember(customerProfile) { mutableStateOf(customerProfile?.phone ?: "") }
    var callbackTopic by remember { mutableStateOf("") }
    var callbackSubmittedMessage by remember { mutableStateOf<String?>(null) }

    // Scroll to bottom when messages update
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
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
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Support & Assistance",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = if (chatMode == "human") "Staff Specialist Connected" else "Tour Manage AI Assistant",
                                fontSize = 11.sp,
                                color = if (chatMode == "human") StatusGreen else TourNavy
                            )
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
            // Tabs: Chat vs Contact/Callback
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Live Travel Chat", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Direct Contact & Callback", fontWeight = FontWeight.SemiBold) }
                )
            }

            if (selectedTab == 0) {
                // CHAT SECTION
                Column(modifier = Modifier.fillMaxSize()) {
                    // Handover banner & Mode indicator
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (chatMode == "human") StatusGreen else TourGold)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (chatMode == "human") "Responding: Support Staff" else "Responding: AI Assistant",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (chatMode == "bot") {
                                OutlinedButton(
                                    onClick = { repository.requestHumanHandover() },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("talk_to_human_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Talk to a human", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (currentUser?.isDemo == true || authRepo.isDemoSession.collectAsState().value) {
                        Surface(
                            color = TourGoldLight,
                            contentColor = TourGoldDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("demo_mode_chat_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = TourGoldDark,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = "DEMO MODE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "On-device assistant answers from cached destinations, agencies, and packages.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else if (API_BASE_URL.isBlank()) {
                        BackendNotConnectedBanner(feature = "Real-time server chat sync")
                    }

                    // Chat messages list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(chatMessages) { msg ->
                            val isCustomer = msg.sender == "customer"
                            val isStaff = msg.sender == "staff"

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (isCustomer) Alignment.End else Alignment.Start
                            ) {
                                // Sender Label
                                Text(
                                    text = when (msg.sender) {
                                        "customer" -> "You"
                                        "staff" -> "Support Staff • Tour Manage"
                                        else -> "Tour Manage AI Bot"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when (msg.sender) {
                                        "customer" -> MaterialTheme.colorScheme.primary
                                        "staff" -> StatusGreen
                                        else -> TourGoldDark
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )

                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 14.dp,
                                        topEnd = 14.dp,
                                        bottomStart = if (isCustomer) 14.dp else 2.dp,
                                        bottomEnd = if (isCustomer) 2.dp else 14.dp
                                    ),
                                    color = when (msg.sender) {
                                        "customer" -> MaterialTheme.colorScheme.primary
                                        "staff" -> Color(0xFFDCFCE7)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    contentColor = when (msg.sender) {
                                        "customer" -> MaterialTheme.colorScheme.onPrimary
                                        "staff" -> Color(0xFF14532D)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.widthIn(max = 300.dp)
                                ) {
                                    Text(
                                        text = msg.text,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }

                                if (msg.createdAt != null) {
                                    Text(
                                        text = formatTimestampDateTime(msg.createdAt),
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Input Row
                    Surface(
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = messageInput,
                                onValueChange = { messageInput = it },
                                placeholder = { Text("Ask about packages, custom dates, diets...") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input_field"),
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 3
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (messageInput.isNotBlank()) {
                                        val text = messageInput
                                        messageInput = ""
                                        scope.launch {
                                            val customerId = currentUser?.uid ?: "guest"
                                            repository.sendChatMessage(customerId, text)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .testTag("send_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // CONTACT & CALLBACK SECTION
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Support Hours & Email
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = companySettings.companyName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = TourNavy
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = companySettings.supportHours,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = TourNavy
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = companySettings.supportEmail,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Tap-to-call buttons for every number in settings/company.supportPhones
                    item {
                        Text(
                            text = "Direct Phone Support",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            companySettings.supportPhones.forEach { phone ->
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Phone, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Call Support: $phone")
                                }
                            }

                            // WhatsApp Button
                            Button(
                                onClick = {
                                    try {
                                        val cleanNum = companySettings.whatsappNumber.replace("+", "").replace(" ", "")
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanNum&text=Hi%20Tour%20Manage,%20I%20have%20an%20inquiry%20regarding%20tour%20packages."))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF25D366),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Chat, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Chat on WhatsApp")
                            }
                        }
                    }

                    // Callback Request Form
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Request an Instant Callback",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Our certified tour advisors across India will call you back within 15 minutes.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = callbackName,
                                    onValueChange = { callbackName = it },
                                    label = { Text("Your Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = callbackPhone,
                                    onValueChange = { callbackPhone = it },
                                    label = { Text("Phone Number (+91)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = callbackTopic,
                                    onValueChange = { callbackTopic = it },
                                    label = { Text("Destination / Query (e.g. Kashmir 6-day package)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    maxLines = 2
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                if (callbackSubmittedMessage != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFDCFCE7),
                                        contentColor = Color(0xFF14532D),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = callbackSubmittedMessage!!,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Button(
                                    onClick = {
                                        if (callbackName.isNotBlank() && callbackPhone.isNotBlank()) {
                                            scope.launch {
                                                val res = repository.requestCallback(
                                                    customerId = currentUser?.uid ?: "guest",
                                                    name = callbackName,
                                                    phone = callbackPhone,
                                                    topic = callbackTopic
                                                )
                                                callbackSubmittedMessage = res.getOrNull() ?: "Callback request submitted!"
                                            }
                                        } else {
                                            Toast.makeText(context, "Please enter your name and phone number", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Submit Callback Request", fontWeight = FontWeight.Bold)
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
    }
}
