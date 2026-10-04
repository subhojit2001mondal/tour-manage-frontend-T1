package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.API_BASE_URL
import com.example.data.FirebaseProvider
import com.example.data.TourNotificationHelper
import com.example.data.models.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Date
import java.util.UUID

class TourRepository(
    private val context: Context,
    private val authRepo: AuthRepository
) {
    private val TAG = "TourRepository"
    private val firestore: FirebaseFirestore by lazy { FirebaseProvider.getFirestore(context) }
    private val httpClient = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.IO)

    // Shared StateFlows with WhileSubscribed
    private val _destinations = MutableStateFlow<List<Destination>>(emptyList())
    val destinations: StateFlow<List<Destination>> = _destinations.asStateFlow()

    private val _agencies = MutableStateFlow<List<Agency>>(emptyList())
    val agencies: StateFlow<List<Agency>> = _agencies.asStateFlow()

    private val _packages = MutableStateFlow<List<TourPackage>>(emptyList())
    val packages: StateFlow<List<TourPackage>> = _packages.asStateFlow()

    private val _departures = MutableStateFlow<List<Departure>>(emptyList())
    val departures: StateFlow<List<Departure>> = _departures.asStateFlow()

    private val _companySettings = MutableStateFlow(CompanySettings())
    val companySettings: StateFlow<CompanySettings> = _companySettings.asStateFlow()

    private val _comparePackageIds = MutableStateFlow<Set<String>>(emptySet())
    val comparePackageIds: StateFlow<Set<String>> = _comparePackageIds.asStateFlow()

    private val _myBookings = MutableStateFlow<List<Booking>>(emptyList())
    val myBookings: StateFlow<List<Booking>> = _myBookings.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chatMode = MutableStateFlow("bot")
    val chatMode: StateFlow<String> = _chatMode.asStateFlow()

    // Wishlist per customer
    private val _wishlist = MutableStateFlow<List<WishlistItem>>(emptyList())
    val wishlist: StateFlow<List<WishlistItem>> = _wishlist.asStateFlow()

    // Notifications per customer
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _notificationPreferences = MutableStateFlow(NotificationPreferences())
    val notificationPreferences: StateFlow<NotificationPreferences> = _notificationPreferences.asStateFlow()

    // Activity & History per customer
    private val _searchHistory = MutableStateFlow<List<SearchHistoryItem>>(emptyList())
    val searchHistory: StateFlow<List<SearchHistoryItem>> = _searchHistory.asStateFlow()

    private val _recentlyViewed = MutableStateFlow<List<RecentlyViewedItem>>(emptyList())
    val recentlyViewed: StateFlow<List<RecentlyViewedItem>> = _recentlyViewed.asStateFlow()

    // Precomputed Calendar Departures Map (Year, Month, Day) -> Departures
    val calendarDepartureMap: StateFlow<Map<Triple<Int, Int, Int>, List<Departure>>> =
        _departures.map { depList ->
            val map = mutableMapOf<Triple<Int, Int, Int>, MutableList<Departure>>()
            val cal = Calendar.getInstance()
            depList.forEach { dep ->
                dep.date?.let { ts ->
                    cal.time = ts.toDate()
                    val key = Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                    map.getOrPut(key) { mutableListOf() }.add(dep)
                }
            }
            map
        }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _firestoreError = MutableStateFlow<String?>(null)
    val firestoreError: StateFlow<String?> = _firestoreError.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Listener registrations
    private var destinationsListener: ListenerRegistration? = null
    private var agenciesListener: ListenerRegistration? = null
    private var packagesListener: ListenerRegistration? = null
    private var departuresListener: ListenerRegistration? = null
    private var companyListener: ListenerRegistration? = null
    private var bookingsListener: ListenerRegistration? = null
    private var chatListener: ListenerRegistration? = null

    // Track previous states for notification triggers
    private val knownBookingStatuses = mutableMapOf<String, String>()
    private var lastKnownMessageCount = 0

    init {
        TourNotificationHelper.createChannels(context)
        loadSavedCompare()
        setupListeners()

        // Observe Auth changes to load customer-specific data
        scope.launch {
            authRepo.currentUser.collect { user ->
                if (user != null) {
                    val uid = user.uid
                    loadCustomerLocalData(uid)
                    setupCustomerBookingsListener(uid)
                    setupCustomerChatListener(uid)
                } else {
                    bookingsListener?.remove()
                    chatListener?.remove()
                    _myBookings.value = emptyList()
                    _chatMessages.value = emptyList()
                    _wishlist.value = emptyList()
                    _notifications.value = emptyList()
                    _searchHistory.value = emptyList()
                    _recentlyViewed.value = emptyList()
                }
            }
        }

        // Live monitor for price drops & low seats on wishlisted packages
        scope.launch {
            combine(_packages, _departures, _wishlist) { pkgs, deps, wish ->
                Triple(pkgs, deps, wish)
            }.collect { (pkgs, deps, wish) ->
                evaluateWishlistAlerts(pkgs, deps, wish)
            }
        }
    }

    private fun getCustomerPrefs(uid: String) =
        context.getSharedPreferences("tour_customer_$uid", Context.MODE_PRIVATE)

    private fun loadCustomerLocalData(uid: String) {
        val prefs = getCustomerPrefs(uid)

        // Load Wishlist
        val wishJson = prefs.getString("wishlist_json", "[]") ?: "[]"
        try {
            val arr = JSONArray(wishJson)
            val list = mutableListOf<WishlistItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    WishlistItem(
                        packageId = obj.getString("packageId"),
                        savedPrice = obj.optLong("savedPrice", 0L),
                        savedAt = obj.optLong("savedAt", System.currentTimeMillis())
                    )
                )
            }
            _wishlist.value = list
        } catch (e: Exception) {
            _wishlist.value = emptyList()
        }

        // Load Notifications
        val notifJson = prefs.getString("notif_json", "[]") ?: "[]"
        try {
            val arr = JSONArray(notifJson)
            val list = mutableListOf<NotificationItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    NotificationItem(
                        id = obj.getString("id"),
                        customerId = uid,
                        title = obj.getString("title"),
                        message = obj.getString("message"),
                        type = obj.optString("type", "booking"),
                        targetType = obj.optString("targetType", "booking"),
                        targetId = obj.optString("targetId", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        isRead = obj.optBoolean("isRead", false)
                    )
                )
            }
            _notifications.value = list
        } catch (e: Exception) {
            _notifications.value = emptyList()
        }

        // Load Searches
        val searchJson = prefs.getString("searches_json", "[]") ?: "[]"
        try {
            val arr = JSONArray(searchJson)
            val list = mutableListOf<SearchHistoryItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SearchHistoryItem(
                        id = obj.getString("id"),
                        query = obj.getString("query"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            _searchHistory.value = list
        } catch (e: Exception) {
            _searchHistory.value = emptyList()
        }

        // Load Recently Viewed
        val recentJson = prefs.getString("recent_json", "[]") ?: "[]"
        try {
            val arr = JSONArray(recentJson)
            val list = mutableListOf<RecentlyViewedItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    RecentlyViewedItem(
                        packageId = obj.getString("packageId"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            _recentlyViewed.value = list
        } catch (e: Exception) {
            _recentlyViewed.value = emptyList()
        }

        // Load Notification Preferences
        _notificationPreferences.value = NotificationPreferences(
            bookingUpdates = prefs.getBoolean("pref_bookings", true),
            chatMessages = prefs.getBoolean("pref_chat", true),
            priceAlerts = prefs.getBoolean("pref_alerts", true),
            tripReminders = prefs.getBoolean("pref_reminders", true)
        )
    }

    // WISHLIST OPERATIONS
    fun toggleWishlist(packageId: String): Pair<Boolean, WishlistItem?> {
        val uid = authRepo.currentUser.value?.uid ?: return Pair(false, null)
        val current = _wishlist.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.packageId == packageId }

        val pkg = _packages.value.find { it.id == packageId }
        val price = pkg?.pricePerPerson ?: 0L

        return if (existingIndex >= 0) {
            val removedItem = current.removeAt(existingIndex)
            _wishlist.value = current
            saveWishlist(uid, current)
            Pair(false, removedItem)
        } else {
            val newItem = WishlistItem(packageId = packageId, savedPrice = price, savedAt = System.currentTimeMillis())
            current.add(0, newItem)
            _wishlist.value = current
            saveWishlist(uid, current)
            Pair(true, newItem)
        }
    }

    fun restoreWishlistItem(item: WishlistItem) {
        val uid = authRepo.currentUser.value?.uid ?: return
        val current = _wishlist.value.toMutableList()
        if (current.none { it.packageId == item.packageId }) {
            current.add(0, item)
            _wishlist.value = current
            saveWishlist(uid, current)
        }
    }

    fun removeFromWishlist(packageId: String) {
        val uid = authRepo.currentUser.value?.uid ?: return
        val current = _wishlist.value.toMutableList()
        current.removeAll { it.packageId == packageId }
        _wishlist.value = current
        saveWishlist(uid, current)
    }

    fun isWishlisted(packageId: String): Boolean {
        return _wishlist.value.any { it.packageId == packageId }
    }

    private fun saveWishlist(uid: String, list: List<WishlistItem>) {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(JSONObject().apply {
                put("packageId", item.packageId)
                put("savedPrice", item.savedPrice)
                put("savedAt", item.savedAt)
            })
        }
        getCustomerPrefs(uid).edit().putString("wishlist_json", arr.toString()).apply()
    }

    // SEARCH HISTORY OPERATIONS
    fun addSearchQuery(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        val uid = authRepo.currentUser.value?.uid ?: return
        val current = _searchHistory.value.toMutableList()
        current.removeAll { it.query.equals(clean, ignoreCase = true) }
        current.add(0, SearchHistoryItem(id = UUID.randomUUID().toString(), query = clean, timestamp = System.currentTimeMillis()))
        val trimmed = current.take(20)
        _searchHistory.value = trimmed
        saveSearchHistory(uid, trimmed)
    }

    fun removeSearchItem(id: String) {
        val uid = authRepo.currentUser.value?.uid ?: return
        val current = _searchHistory.value.toMutableList()
        current.removeAll { it.id == id }
        _searchHistory.value = current
        saveSearchHistory(uid, current)
    }

    fun clearSearchHistory() {
        val uid = authRepo.currentUser.value?.uid ?: return
        _searchHistory.value = emptyList()
        saveSearchHistory(uid, emptyList())
    }

    private fun saveSearchHistory(uid: String, list: List<SearchHistoryItem>) {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("query", item.query)
                put("timestamp", item.timestamp)
            })
        }
        getCustomerPrefs(uid).edit().putString("searches_json", arr.toString()).apply()
    }

    // RECENTLY VIEWED OPERATIONS
    fun addRecentlyViewed(packageId: String) {
        val uid = authRepo.currentUser.value?.uid ?: return
        val current = _recentlyViewed.value.toMutableList()
        current.removeAll { it.packageId == packageId }
        current.add(0, RecentlyViewedItem(packageId = packageId, timestamp = System.currentTimeMillis()))
        val trimmed = current.take(20)
        _recentlyViewed.value = trimmed
        saveRecentlyViewed(uid, trimmed)
    }

    fun clearRecentlyViewed() {
        val uid = authRepo.currentUser.value?.uid ?: return
        _recentlyViewed.value = emptyList()
        saveRecentlyViewed(uid, emptyList())
    }

    private fun saveRecentlyViewed(uid: String, list: List<RecentlyViewedItem>) {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(JSONObject().apply {
                put("packageId", item.packageId)
                put("timestamp", item.timestamp)
            })
        }
        getCustomerPrefs(uid).edit().putString("recent_json", arr.toString()).apply()
    }

    // NOTIFICATIONS OPERATIONS
    fun markNotificationAsRead(id: String) {
        val uid = authRepo.currentUser.value?.uid ?: return
        val current = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        _notifications.value = current
        saveNotifications(uid, current)
    }

    fun markAllNotificationsAsRead() {
        val uid = authRepo.currentUser.value?.uid ?: return
        val current = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = current
        saveNotifications(uid, current)
    }

    fun clearNotifications() {
        val uid = authRepo.currentUser.value?.uid ?: return
        _notifications.value = emptyList()
        saveNotifications(uid, emptyList())
    }

    fun updateNotificationPreferences(prefs: NotificationPreferences) {
        val uid = authRepo.currentUser.value?.uid ?: return
        _notificationPreferences.value = prefs
        getCustomerPrefs(uid).edit()
            .putBoolean("pref_bookings", prefs.bookingUpdates)
            .putBoolean("pref_chat", prefs.chatMessages)
            .putBoolean("pref_alerts", prefs.priceAlerts)
            .putBoolean("pref_reminders", prefs.tripReminders)
            .apply()
    }

    private fun addNotificationInternal(
        uid: String,
        title: String,
        message: String,
        type: String,
        targetType: String,
        targetId: String,
        channelId: String
    ) {
        val current = _notifications.value.toMutableList()
        // Prevent duplicate notification within last 12 hours
        val isDuplicate = current.any {
            it.type == type && it.targetId == targetId && (System.currentTimeMillis() - it.createdAt < 12 * 3600 * 1000L)
        }
        if (isDuplicate) return

        val notif = NotificationItem(
            id = UUID.randomUUID().toString(),
            customerId = uid,
            title = title,
            message = message,
            type = type,
            targetType = targetType,
            targetId = targetId,
            createdAt = System.currentTimeMillis(),
            isRead = false
        )
        current.add(0, notif)
        val trimmed = current.take(50)
        _notifications.value = trimmed
        saveNotifications(uid, trimmed)

        // Dispatch Android system notification
        TourNotificationHelper.postNotification(
            context = context,
            channelId = channelId,
            notificationId = notif.id.hashCode(),
            title = title,
            message = message
        )
    }

    private fun saveNotifications(uid: String, list: List<NotificationItem>) {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("message", item.message)
                put("type", item.type)
                put("targetType", item.targetType)
                put("targetId", item.targetId)
                put("createdAt", item.createdAt)
                put("isRead", item.isRead)
            })
        }
        getCustomerPrefs(uid).edit().putString("notif_json", arr.toString()).apply()
    }

    private fun evaluateWishlistAlerts(pkgs: List<TourPackage>, deps: List<Departure>, wish: List<WishlistItem>) {
        val uid = authRepo.currentUser.value?.uid ?: return
        if (!_notificationPreferences.value.priceAlerts) return

        wish.forEach { wishItem ->
            val pkg = pkgs.find { it.id == wishItem.packageId } ?: return@forEach
            // Price drop alert
            if (wishItem.savedPrice > 0 && pkg.pricePerPerson < wishItem.savedPrice) {
                val drop = wishItem.savedPrice - pkg.pricePerPerson
                addNotificationInternal(
                    uid = uid,
                    title = "Price Drop: ${pkg.title}",
                    message = "Good news! Tour price dropped by ${formatInr(drop)}. Current price is ${formatInr(pkg.pricePerPerson)}.",
                    type = "wishlist_price",
                    targetType = "package",
                    targetId = pkg.id,
                    channelId = TourNotificationHelper.CHANNEL_ID_ALERTS
                )
            }

            // Seats alert
            val nextDep = deps.firstOrNull { it.packageId == pkg.id && it.date != null && it.date.toDate().after(Date()) }
            if (nextDep != null) {
                if (nextDep.seatsLeft in 1..3) {
                    addNotificationInternal(
                        uid = uid,
                        title = "Only ${nextDep.seatsLeft} Seats Left: ${pkg.title}",
                        message = "Departure on ${formatTimestampDate(nextDep.date)} is filling fast. Book soon to secure seats!",
                        type = "wishlist_seats",
                        targetType = "package",
                        targetId = pkg.id,
                        channelId = TourNotificationHelper.CHANNEL_ID_ALERTS
                    )
                } else if (nextDep.seatsLeft == 0 || nextDep.status == "full") {
                    addNotificationInternal(
                        uid = uid,
                        title = "Sold Out: ${pkg.title}",
                        message = "Departure on ${formatTimestampDate(nextDep.date)} has sold out. View alternative dates.",
                        type = "wishlist_seats",
                        targetType = "package",
                        targetId = pkg.id,
                        channelId = TourNotificationHelper.CHANNEL_ID_ALERTS
                    )
                }
            }
        }
    }

    private fun evaluateBookingNotifications(newBookings: List<Booking>) {
        val uid = authRepo.currentUser.value?.uid ?: return
        if (!_notificationPreferences.value.bookingUpdates) return

        newBookings.forEach { booking ->
            val oldStatus = knownBookingStatuses[booking.id]
            if (oldStatus != null && oldStatus != booking.status) {
                addNotificationInternal(
                    uid = uid,
                    title = "Booking ${booking.bookingCode} ${booking.status.replaceFirstChar { it.uppercase() }}",
                    message = "Your tour booking status for ${booking.packageTitle} is now '${booking.status}'.",
                    type = "booking",
                    targetType = "booking",
                    targetId = booking.id,
                    channelId = TourNotificationHelper.CHANNEL_ID_BOOKINGS
                )
            }
            knownBookingStatuses[booking.id] = booking.status

            // Trip reminder check (3 days and 1 day)
            if (_notificationPreferences.value.tripReminders && booking.travelDate != null) {
                val travelMillis = booking.travelDate.toDate().time
                val diffDays = ((travelMillis - System.currentTimeMillis()) / (24 * 3600 * 1000L)).toInt()
                if (diffDays in 1..3 && booking.status == "confirmed") {
                    addNotificationInternal(
                        uid = uid,
                        title = "Upcoming Trip Reminder ($diffDays Days Away)",
                        message = "Get ready for ${booking.packageTitle}! Departure is on ${formatTimestampDate(booking.travelDate)}.",
                        type = "trip_reminder",
                        targetType = "booking",
                        targetId = booking.id,
                        channelId = TourNotificationHelper.CHANNEL_ID_BOOKINGS
                    )
                }
            }
        }
    }

    // COMPARE STORAGE
    private fun loadSavedCompare() {
        val saved = context.getSharedPreferences("tour_manage_compare", Context.MODE_PRIVATE)
            .getStringSet("compare_ids", emptySet()) ?: emptySet()
        _comparePackageIds.value = saved
    }

    fun toggleCompare(packageId: String): Boolean {
        val current = _comparePackageIds.value.toMutableSet()
        val added: Boolean
        if (current.contains(packageId)) {
            current.remove(packageId)
            added = false
        } else {
            if (current.size >= 4) return false
            current.add(packageId)
            added = true
        }
        _comparePackageIds.value = current
        context.getSharedPreferences("tour_manage_compare", Context.MODE_PRIVATE)
            .edit().putStringSet("compare_ids", current).apply()
        return added
    }

    fun removeFromCompare(packageId: String) {
        val current = _comparePackageIds.value.toMutableSet()
        current.remove(packageId)
        _comparePackageIds.value = current
        context.getSharedPreferences("tour_manage_compare", Context.MODE_PRIVATE)
            .edit().putStringSet("compare_ids", current).apply()
    }

    fun clearCompare() {
        _comparePackageIds.value = emptySet()
        context.getSharedPreferences("tour_manage_compare", Context.MODE_PRIVATE)
            .edit().remove("compare_ids").apply()
    }

    fun clearError() {
        _firestoreError.value = null
    }

    fun refreshAll() {
        setupListeners()
        authRepo.currentUser.value?.uid?.let { uid ->
            setupCustomerBookingsListener(uid)
            setupCustomerChatListener(uid)
        }
    }

    // SHARED FIRESTORE LISTENERS
    private fun setupListeners() {
        _isLoading.value = true
        try {
            // 1. Destinations Listener
            destinationsListener?.remove()
            destinationsListener = firestore.collection("destinations")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Destinations listen error", error)
                        _firestoreError.value = "Destinations Firestore error: ${error.localizedMessage ?: error.message}"
                        if (_destinations.value.isEmpty()) {
                            _destinations.value = SampleTourData.getSampleDestinations()
                        }
                    } else if (snapshot != null) {
                        if (snapshot.metadata.hasPendingWrites()) return@addSnapshotListener
                        val items = snapshot.documents.mapNotNull { parseDestination(it) }
                        if (items.isNotEmpty()) {
                            _destinations.value = items
                        } else if (_destinations.value.isEmpty()) {
                            _destinations.value = SampleTourData.getSampleDestinations()
                        }
                    }
                    _isLoading.value = false
                }

            // 2. Agencies Listener
            agenciesListener?.remove()
            agenciesListener = firestore.collection("agencies")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Agencies listen error", error)
                        _firestoreError.value = "Agencies Firestore error: ${error.localizedMessage ?: error.message}"
                        if (_agencies.value.isEmpty()) {
                            _agencies.value = SampleTourData.getSampleAgencies()
                        }
                    } else if (snapshot != null) {
                        if (snapshot.metadata.hasPendingWrites()) return@addSnapshotListener
                        val items = snapshot.documents.mapNotNull { parseAgency(it) }
                        if (items.isNotEmpty()) {
                            _agencies.value = items
                        } else if (_agencies.value.isEmpty()) {
                            _agencies.value = SampleTourData.getSampleAgencies()
                        }
                    }
                }

            // 3. Packages Listener
            packagesListener?.remove()
            packagesListener = firestore.collection("packages")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Packages listen error", error)
                        _firestoreError.value = "Packages Firestore error: ${error.localizedMessage ?: error.message}"
                        if (_packages.value.isEmpty()) {
                            _packages.value = SampleTourData.getSamplePackages()
                        }
                    } else if (snapshot != null) {
                        if (snapshot.metadata.hasPendingWrites()) return@addSnapshotListener
                        val items = snapshot.documents.mapNotNull { parsePackage(it) }
                        if (items.isNotEmpty()) {
                            _packages.value = items
                        } else if (_packages.value.isEmpty()) {
                            _packages.value = SampleTourData.getSamplePackages()
                        }
                    }
                }

            // 4. Departures Listener (next 90 days limit to fix lag)
            departuresListener?.remove()
            departuresListener = firestore.collection("departures")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Departures listen error", error)
                        _firestoreError.value = "Departures Firestore error: ${error.localizedMessage ?: error.message}"
                        if (_departures.value.isEmpty()) {
                            _departures.value = SampleTourData.getSampleDepartures()
                        }
                    } else if (snapshot != null) {
                        if (snapshot.metadata.hasPendingWrites()) return@addSnapshotListener
                        val nowMillis = System.currentTimeMillis()
                        val maxMillis = nowMillis + 90L * 24 * 3600 * 1000L
                        val items = snapshot.documents.mapNotNull { parseDeparture(it) }
                            .filter {
                                val depTime = it.date?.toDate()?.time ?: nowMillis
                                depTime in (nowMillis - 86400000L)..maxMillis
                            }
                        if (items.isNotEmpty()) {
                            _departures.value = items
                        } else if (_departures.value.isEmpty()) {
                            _departures.value = SampleTourData.getSampleDepartures()
                        }
                    }
                }

            // 5. Company Settings Listener
            companyListener?.remove()
            companyListener = firestore.collection("settings").document("company")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && snapshot.exists()) {
                        if (snapshot.metadata.hasPendingWrites()) return@addSnapshotListener
                        val settings = CompanySettings(
                            companyName = snapshot.getString("companyName") ?: "Tour Manage",
                            supportPhones = (snapshot.get("supportPhones") as? List<*>)?.mapNotNull { it?.toString() }
                                ?: listOf("+91 98765 43210", "+91 98765 43211"),
                            supportEmail = snapshot.getString("supportEmail") ?: "support@tourmanage.com",
                            supportHours = snapshot.getString("supportHours") ?: "Mon - Sun: 8:00 AM - 10:00 PM IST",
                            whatsappNumber = snapshot.getString("whatsappNumber") ?: "+919876543210",
                            aboutText = snapshot.getString("aboutText") ?: "Tour Manage is India's premier tour marketplace.",
                            termsUrl = snapshot.getString("termsUrl") ?: "https://tourmanage.com/terms"
                        )
                        _companySettings.value = settings
                    }
                }

        } catch (e: Exception) {
            Log.e(TAG, "Setup listeners exception", e)
            _firestoreError.value = "Firestore Init Error: ${e.localizedMessage ?: e.message}"
            _destinations.value = SampleTourData.getSampleDestinations()
            _agencies.value = SampleTourData.getSampleAgencies()
            _packages.value = SampleTourData.getSamplePackages()
            _departures.value = SampleTourData.getSampleDepartures()
            _isLoading.value = false
        }
    }

    private fun setupCustomerBookingsListener(customerId: String) {
        bookingsListener?.remove()
        try {
            bookingsListener = firestore.collection("bookings")
                .whereEqualTo("customerId", customerId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Customer bookings error: ${error.message}")
                        _firestoreError.value = "Bookings error: ${error.localizedMessage ?: error.message}"
                    } else if (snapshot != null) {
                        if (snapshot.metadata.hasPendingWrites()) return@addSnapshotListener
                        val list = snapshot.documents.mapNotNull { parseBooking(it) }
                        _myBookings.value = list
                        evaluateBookingNotifications(list)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Customer bookings listen error", e)
        }
    }

    private fun setupCustomerChatListener(customerId: String) {
        chatListener?.remove()
        try {
            val chatId = "chat_$customerId"
            chatListener = firestore.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("createdAt")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && !snapshot.isEmpty) {
                        if (snapshot.metadata.hasPendingWrites()) return@addSnapshotListener
                        val messages = snapshot.documents.mapNotNull { doc ->
                            ChatMessage(
                                id = doc.id,
                                sender = doc.getString("sender") ?: "bot",
                                text = doc.getString("text") ?: "",
                                createdAt = doc.getTimestamp("createdAt")
                            )
                        }
                        _chatMessages.value = messages

                        // Check if new staff reply arrived
                        if (messages.size > lastKnownMessageCount) {
                            val latest = messages.last()
                            if (latest.sender == "staff" && _notificationPreferences.value.chatMessages) {
                                addNotificationInternal(
                                    uid = customerId,
                                    title = "Support Team Replied",
                                    message = latest.text.take(80),
                                    type = "chat",
                                    targetType = "chat",
                                    targetId = chatId,
                                    channelId = TourNotificationHelper.CHANNEL_ID_CHAT
                                )
                            }
                        }
                        lastKnownMessageCount = messages.size
                    } else if (_chatMessages.value.isEmpty()) {
                        _chatMessages.value = listOf(
                            ChatMessage(
                                id = "msg_welcome",
                                sender = "bot",
                                text = "Namaste! 🙏 Welcome to Tour Manage India. I am your AI Travel Assistant. How can I help you discover or customize your dream journey across India?",
                                createdAt = Timestamp.now()
                            )
                        )
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Customer chat listen error", e)
        }
    }

    // CHAT ACTIONS
    suspend fun sendChatMessage(customerId: String, text: String): Result<String> = withContext(Dispatchers.IO) {
        val currentList = _chatMessages.value.toMutableList()
        val userMsg = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            sender = "customer",
            text = text,
            createdAt = Timestamp.now()
        )
        currentList.add(userMsg)
        _chatMessages.value = currentList

        if (API_BASE_URL.isNotBlank()) {
            try {
                val token = authRepo.getIdToken() ?: ""
                val chatId = "chat_$customerId"
                val json = JSONObject().apply {
                    put("sender", "customer")
                    put("text", text)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$API_BASE_URL/api/chats/$chatId/messages")
                    .addHeader("Authorization", "Bearer $token")
                    .post(body)
                    .build()
                val response = httpClient.newCall(request).execute()
                response.close()
                Result.success("Message sent")
            } catch (e: Exception) {
                Log.e(TAG, "API chat message error", e)
                Result.failure(e)
            }
        } else {
            val replyText = generateBotReply(text, _chatMode.value)
            val botMsg = ChatMessage(
                id = "bot_${System.currentTimeMillis()}",
                sender = if (_chatMode.value == "human") "staff" else "bot",
                text = replyText,
                createdAt = Timestamp.now()
            )
            val updated = _chatMessages.value.toMutableList()
            updated.add(botMsg)
            _chatMessages.value = updated
            Result.success("Local assistant responded")
        }
    }

    fun requestHumanHandover() {
        _chatMode.value = "human"
        val updated = _chatMessages.value.toMutableList()
        updated.add(
            ChatMessage(
                id = "handover_${System.currentTimeMillis()}",
                sender = "staff",
                text = "Hello! A Tour Manage support specialist has joined the chat. How may we assist you with your booking, vehicle requests, or special arrangements?",
                createdAt = Timestamp.now()
            )
        )
        _chatMessages.value = updated
    }

    suspend fun requestCallback(
        customerId: String,
        name: String,
        phone: String,
        topic: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (API_BASE_URL.isNotBlank()) {
            try {
                val token = authRepo.getIdToken() ?: ""
                val json = JSONObject().apply {
                    put("customerId", customerId)
                    put("name", name)
                    put("phone", phone)
                    put("topic", topic)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$API_BASE_URL/api/callbacks")
                    .addHeader("Authorization", "Bearer $token")
                    .post(body)
                    .build()
                val response = httpClient.newCall(request).execute()
                response.close()
                Result.success("Callback requested successfully!")
            } catch (e: Exception) {
                Log.e(TAG, "Callback request failed", e)
                Result.failure(e)
            }
        } else {
            Result.success("Callback request submitted! Our travel expert will call you at $phone shortly.")
        }
    }

    // BOOKING OPERATIONS
    suspend fun holdBooking(
        pkg: TourPackage,
        departure: Departure,
        travelers: List<Traveler>,
        notes: String
    ): Result<BookingHoldResponse> = withContext(Dispatchers.IO) {
        val totalAmount = pkg.pricePerPerson * travelers.size
        val code = "TM-" + (100000..999999).random()

        if (API_BASE_URL.isNotBlank()) {
            try {
                val token = authRepo.getIdToken() ?: ""
                val json = JSONObject().apply {
                    put("packageId", pkg.id)
                    put("agencyId", pkg.agencyId)
                    put("destinationId", pkg.destinationId)
                    put("departureId", departure.id)
                    put("totalAmount", totalAmount)
                    val travelersArr = JSONArray()
                    travelers.forEach { t ->
                        travelersArr.put(JSONObject().apply {
                            put("name", t.name)
                            put("age", t.age)
                        })
                    }
                    put("travelers", travelersArr)
                    put("notes", notes)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$API_BASE_URL/api/bookings/hold")
                    .addHeader("Authorization", "Bearer $token")
                    .post(body)
                    .build()
                val response = httpClient.newCall(request).execute()
                val resString = response.body?.string() ?: "{}"
                val resJson = JSONObject(resString)
                val bookingId = resJson.optString("bookingId", UUID.randomUUID().toString())
                val bookingCode = resJson.optString("bookingCode", code)
                val mode = resJson.optString("mode", "demo")
                val razorpayOrderId = resJson.optString("razorpayOrderId", null)

                Result.success(
                    BookingHoldResponse(
                        bookingId = bookingId,
                        bookingCode = bookingCode,
                        mode = mode,
                        totalAmount = totalAmount,
                        razorpayOrderId = razorpayOrderId
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Hold booking API failed", e)
                Result.failure(e)
            }
        } else {
            val bId = "demo_bk_${System.currentTimeMillis()}"
            Result.success(
                BookingHoldResponse(
                    bookingId = bId,
                    bookingCode = code,
                    mode = "demo",
                    totalAmount = totalAmount,
                    razorpayOrderId = null
                )
            )
        }
    }

    suspend fun verifyBooking(
        bookingId: String,
        bookingCode: String,
        pkg: TourPackage,
        departure: Departure,
        travelers: List<Traveler>,
        totalAmount: Long,
        demoResult: Boolean
    ): Result<Booking> = withContext(Dispatchers.IO) {
        val user = authRepo.currentUser.value
        val customerId = user?.uid ?: "guest_${System.currentTimeMillis()}"

        if (API_BASE_URL.isNotBlank()) {
            try {
                val token = authRepo.getIdToken() ?: ""
                val json = JSONObject().apply {
                    put("bookingId", bookingId)
                    put("demoResult", if (demoResult) "success" else "failed")
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$API_BASE_URL/api/bookings/verify")
                    .addHeader("Authorization", "Bearer $token")
                    .post(body)
                    .build()
                val response = httpClient.newCall(request).execute()
                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "Verify API error", e)
            }
        }

        if (demoResult) {
            val destination = _destinations.value.find { it.id == pkg.destinationId }
            val agency = _agencies.value.find { it.id == pkg.agencyId }
            val confirmedBooking = Booking(
                id = bookingId,
                bookingCode = bookingCode,
                customerId = customerId,
                packageId = pkg.id,
                agencyId = pkg.agencyId,
                destinationId = pkg.destinationId,
                departureId = departure.id,
                travelDate = departure.date ?: Timestamp.now(),
                travelers = travelers,
                totalAmount = totalAmount,
                commissionAmount = (totalAmount * 0.1).toLong(),
                status = "confirmed",
                paymentStatus = "paid",
                holdExpiresAt = null,
                createdAt = Timestamp.now(),
                packageTitle = pkg.title,
                destinationName = destination?.name ?: "India",
                agencyName = agency?.name ?: "Partner Agency"
            )
            val current = _myBookings.value.toMutableList()
            current.add(0, confirmedBooking)
            _myBookings.value = current

            // Add notification
            addNotificationInternal(
                uid = customerId,
                title = "Booking Confirmed: $bookingCode",
                message = "Your tour '${pkg.title}' has been successfully confirmed. Have a memorable trip!",
                type = "booking",
                targetType = "booking",
                targetId = bookingId,
                channelId = TourNotificationHelper.CHANNEL_ID_BOOKINGS
            )

            Result.success(confirmedBooking)
        } else {
            Result.failure(Exception("Payment was declined or failed during checkout. Please try again."))
        }
    }

    private fun generateBotReply(prompt: String, mode: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("price") || lower.contains("cost") || lower.contains("budget") ->
                "All tour package prices listed on Tour Manage are guaranteed direct from our verified local agencies with zero hidden markups. You can also filter packages by your exact budget range on the Home screen!"
            lower.contains("kerala") ->
                "Kerala packages feature tranquil Alleppey houseboats, Munnar tea estates, and authentic Ayurvedic experiences. Both AC Sedan and Deluxe Houseboat inclusions are detailed on the package page!"
            lower.contains("kashmir") ->
                "Our Kashmir packages include Dal Lake Shikara rides, Gulmarg Gondola transfers, and certified local guides. Check the availability calendar for upcoming departures!"
            lower.contains("hotel") || lower.contains("stay") ->
                "Every package has a dedicated 'Hotel' section showing the exact property name, rating category, room type, and included amenities so you know exactly where you'll be resting."
            lower.contains("food") || lower.contains("meal") || lower.contains("jain") || lower.contains("veg") ->
                "We provide clear meal plan details (Breakfast, MAP, or all meals) and cater to pure vegetarian and Jain dietary preferences on request with local fresh cuisine."
            lower.contains("cab") || lower.contains("car") || lower.contains("vehicle") ->
                "Vehicle specifications (AC Sedan, SUV, or Tempo Traveller) include airport/railway pickup and seating capacity. You can verify this in the Vehicle section of each package!"
            else ->
                "Thank you for contacting Tour Manage! I can assist you with comparing tour packages, checking departure dates, customized itineraries, or you can tap 'Talk to a human' to connect with our operations desk."
        }
    }

    // Document Parsers
    private fun parseDestination(doc: DocumentSnapshot): Destination? {
        return try {
            Destination(
                id = doc.id,
                name = doc.getString("name") ?: "",
                state = doc.getString("state") ?: "",
                region = doc.getString("region") ?: "North",
                description = doc.getString("description") ?: "",
                coverImageUrl = doc.getString("coverImageUrl") ?: "",
                galleryUrls = (doc.get("galleryUrls") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                bestSeason = doc.getString("bestSeason") ?: "",
                tags = (doc.get("tags") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                featured = doc.getBoolean("featured") ?: false,
                active = doc.getBoolean("active") ?: true,
                isDemo = doc.getBoolean("isDemo") ?: false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing destination ${doc.id}", e)
            null
        }
    }

    private fun parseAgency(doc: DocumentSnapshot): Agency? {
        return try {
            Agency(
                id = doc.id,
                name = doc.getString("name") ?: "",
                logoUrl = doc.getString("logoUrl") ?: "",
                description = doc.getString("description") ?: "",
                tier = doc.getString("tier") ?: "standard",
                rating = doc.getDouble("rating") ?: 4.5,
                verified = doc.getBoolean("verified") ?: false,
                phone = doc.getString("phone") ?: "",
                email = doc.getString("email") ?: "",
                city = doc.getString("city") ?: "",
                commissionPercent = doc.getDouble("commissionPercent") ?: 10.0,
                active = doc.getBoolean("active") ?: true,
                isDemo = doc.getBoolean("isDemo") ?: false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing agency ${doc.id}", e)
            null
        }
    }

    private fun parsePackage(doc: DocumentSnapshot): TourPackage? {
        return try {
            val vehicleMap = doc.get("vehicle") as? Map<*, *>
            val foodMap = doc.get("food") as? Map<*, *>
            val hotelMap = doc.get("hotel") as? Map<*, *>
            val rawItinerary = doc.get("itinerary") as? List<*>

            val itineraryList = rawItinerary?.mapNotNull { item ->
                (item as? Map<*, *>)?.let { m ->
                    ItineraryDay(
                        day = (m["day"] as? Number)?.toInt() ?: 1,
                        title = m["title"]?.toString() ?: "",
                        details = m["details"]?.toString() ?: ""
                    )
                }
            } ?: emptyList()

            TourPackage(
                id = doc.id,
                agencyId = doc.getString("agencyId") ?: "",
                destinationId = doc.getString("destinationId") ?: "",
                title = doc.getString("title") ?: "",
                days = (doc.get("days") as? Number)?.toInt() ?: 1,
                nights = (doc.get("nights") as? Number)?.toInt() ?: 0,
                pricePerPerson = (doc.get("pricePerPerson") as? Number)?.toLong() ?: 0L,
                inclusions = (doc.get("inclusions") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                exclusions = (doc.get("exclusions") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                itinerary = itineraryList,
                cancellationPolicy = doc.getString("cancellationPolicy") ?: "",
                maxGroupSize = (doc.get("maxGroupSize") as? Number)?.toInt() ?: 12,
                imageUrls = (doc.get("imageUrls") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                rating = doc.getDouble("rating") ?: 4.8,
                active = doc.getBoolean("active") ?: true,
                vehicle = VehicleInfo(
                    type = vehicleMap?.get("type")?.toString() ?: "Sedan",
                    vehicleName = vehicleMap?.get("vehicleName")?.toString() ?: "",
                    ac = vehicleMap?.get("ac") as? Boolean ?: true,
                    seatingCapacity = (vehicleMap?.get("seatingCapacity") as? Number)?.toInt() ?: 4,
                    stationOrAirportPickup = vehicleMap?.get("stationOrAirportPickup") as? Boolean ?: true,
                    details = vehicleMap?.get("details")?.toString() ?: "",
                    imageUrl = vehicleMap?.get("imageUrl")?.toString() ?: ""
                ),
                food = FoodInfo(
                    mealPlan = foodMap?.get("mealPlan")?.toString() ?: "Breakfast only",
                    cuisine = foodMap?.get("cuisine")?.toString() ?: "Veg & Non-veg",
                    jainOnRequest = foodMap?.get("jainOnRequest") as? Boolean ?: false,
                    details = foodMap?.get("details")?.toString() ?: ""
                ),
                hotel = HotelInfo(
                    hotelName = hotelMap?.get("hotelName")?.toString() ?: "",
                    category = hotelMap?.get("category")?.toString() ?: "3-star",
                    roomType = hotelMap?.get("roomType")?.toString() ?: "Deluxe Room",
                    occupancy = hotelMap?.get("occupancy")?.toString() ?: "Double sharing",
                    amenities = (hotelMap?.get("amenities") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                    details = hotelMap?.get("details")?.toString() ?: "",
                    imageUrls = (hotelMap?.get("imageUrls") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                ),
                isDemo = doc.getBoolean("isDemo") ?: false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing package ${doc.id}", e)
            null
        }
    }

    private fun parseDeparture(doc: DocumentSnapshot): Departure? {
        return try {
            Departure(
                id = doc.id,
                packageId = doc.getString("packageId") ?: "",
                agencyId = doc.getString("agencyId") ?: "",
                destinationId = doc.getString("destinationId") ?: "",
                date = doc.getTimestamp("date"),
                seatsTotal = (doc.get("seatsTotal") as? Number)?.toInt() ?: 20,
                seatsBooked = (doc.get("seatsBooked") as? Number)?.toInt() ?: 0,
                seatsHeld = (doc.get("seatsHeld") as? Number)?.toInt() ?: 0,
                priceOverride = (doc.get("priceOverride") as? Number)?.toLong(),
                status = doc.getString("status") ?: "open",
                isDemo = doc.getBoolean("isDemo") ?: false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing departure ${doc.id}", e)
            null
        }
    }

    private fun parseBooking(doc: DocumentSnapshot): Booking? {
        return try {
            val travelersRaw = doc.get("travelers") as? List<*>
            val travelersList = travelersRaw?.mapNotNull { item ->
                (item as? Map<*, *>)?.let { m ->
                    Traveler(
                        name = m["name"]?.toString() ?: "",
                        age = (m["age"] as? Number)?.toInt() ?: 25
                    )
                }
            } ?: emptyList()

            val pkgId = doc.getString("packageId") ?: ""
            val destId = doc.getString("destinationId") ?: ""
            val agId = doc.getString("agencyId") ?: ""

            val pkg = _packages.value.find { it.id == pkgId }
            val dest = _destinations.value.find { it.id == destId }
            val agency = _agencies.value.find { it.id == agId }

            Booking(
                id = doc.id,
                bookingCode = doc.getString("bookingCode") ?: "",
                customerId = doc.getString("customerId") ?: "",
                packageId = pkgId,
                agencyId = agId,
                destinationId = destId,
                departureId = doc.getString("departureId") ?: "",
                travelDate = doc.getTimestamp("travelDate"),
                travelers = travelersList,
                totalAmount = (doc.get("totalAmount") as? Number)?.toLong() ?: 0L,
                commissionAmount = (doc.get("commissionAmount") as? Number)?.toLong() ?: 0L,
                status = doc.getString("status") ?: "held",
                paymentStatus = doc.getString("paymentStatus") ?: "pending",
                razorpayOrderId = doc.getString("razorpayOrderId"),
                razorpayPaymentId = doc.getString("razorpayPaymentId"),
                holdExpiresAt = doc.getTimestamp("holdExpiresAt"),
                agencyPayoutStatus = doc.getString("agencyPayoutStatus") ?: "pending",
                notes = doc.getString("notes"),
                createdAt = doc.getTimestamp("createdAt"),
                packageTitle = pkg?.title ?: "India Discovery Tour",
                destinationName = dest?.name ?: "Incredible India",
                agencyName = agency?.name ?: "Verified Agency"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing booking ${doc.id}", e)
            null
        }
    }
}
