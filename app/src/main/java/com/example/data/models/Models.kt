package com.example.data.models

import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Format amounts in INR with Indian digit grouping (e.g. ₹1,25,000)
 */
fun formatInr(amount: Long): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}

fun formatInr(amount: Double): String {
    return formatInr(amount.toLong())
}

fun formatTimestampDate(timestamp: Timestamp?): String {
    if (timestamp == null) return "Flexible / Daily"
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}

fun formatTimestampDateTime(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}

data class Destination(
    val id: String = "",
    val name: String = "",
    val state: String = "",
    val region: String = "", // "North"|"South"|"East"|"West"|"Central"|"Northeast"|"Islands"
    val description: String = "",
    val coverImageUrl: String = "",
    val galleryUrls: List<String> = emptyList(),
    val bestSeason: String = "",
    val tags: List<String> = emptyList(),
    val featured: Boolean = false,
    val active: Boolean = true,
    val isDemo: Boolean = false
)

data class Agency(
    val id: String = "",
    val name: String = "",
    val logoUrl: String = "",
    val description: String = "",
    val tier: String = "standard", // "budget"|"standard"|"premium"
    val rating: Double = 4.8,
    val verified: Boolean = false,
    val phone: String = "",
    val email: String = "",
    val city: String = "",
    val commissionPercent: Double = 10.0,
    val active: Boolean = true,
    val isDemo: Boolean = false
)

data class VehicleInfo(
    val type: String = "Sedan", // "Sedan"|"SUV"|"Tempo Traveller"|"Mini Bus"|"Luxury Coach"|"4x4 Jeep"|"Train + Cab"|"Flight + Cab"|"Boat/Houseboat"|"Other"
    val vehicleName: String = "",
    val ac: Boolean = true,
    val seatingCapacity: Int = 4,
    val stationOrAirportPickup: Boolean = true,
    val details: String = "",
    val imageUrl: String = ""
)

data class FoodInfo(
    val mealPlan: String = "Breakfast only", // "No meals"|"Breakfast only"|"Breakfast + Dinner"|"All meals"
    val cuisine: String = "Veg & Non-veg", // "Veg"|"Non-veg"|"Veg & Non-veg"
    val jainOnRequest: Boolean = false,
    val details: String = ""
)

data class HotelInfo(
    val hotelName: String = "",
    val category: String = "3-star", // "Budget"|"3-star"|"4-star"|"5-star"|"Heritage"|"Resort"|"Homestay"|"Houseboat"|"Camp"
    val roomType: String = "Deluxe Room",
    val occupancy: String = "Double sharing", // "Single"|"Double sharing"|"Triple sharing"
    val amenities: List<String> = emptyList(),
    val details: String = "",
    val imageUrls: List<String> = emptyList()
)

data class ItineraryDay(
    val day: Int = 1,
    val title: String = "",
    val details: String = ""
)

data class TourPackage(
    val id: String = "",
    val agencyId: String = "",
    val destinationId: String = "",
    val title: String = "",
    val days: Int = 1,
    val nights: Int = 1,
    val pricePerPerson: Long = 0L,
    val inclusions: List<String> = emptyList(),
    val exclusions: List<String> = emptyList(),
    val itinerary: List<ItineraryDay> = emptyList(),
    val cancellationPolicy: String = "",
    val maxGroupSize: Int = 12,
    val imageUrls: List<String> = emptyList(),
    val rating: Double = 4.8,
    val active: Boolean = true,
    val vehicle: VehicleInfo = VehicleInfo(),
    val food: FoodInfo = FoodInfo(),
    val hotel: HotelInfo = HotelInfo(),
    val isDemo: Boolean = false
)

data class Departure(
    val id: String = "",
    val packageId: String = "",
    val agencyId: String = "",
    val destinationId: String = "",
    val date: Timestamp? = null,
    val seatsTotal: Int = 20,
    val seatsBooked: Int = 0,
    val seatsHeld: Int = 0,
    val priceOverride: Long? = null,
    val status: String = "open", // "open"|"limited"|"full"|"closed"
    val isDemo: Boolean = false
) {
    val seatsLeft: Int
        get() = (seatsTotal - seatsBooked - seatsHeld).coerceAtLeast(0)
}

data class CustomerProfile(
    val uid: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val createdAt: Timestamp? = null
)

data class Traveler(
    val name: String = "",
    val age: Int = 25
)

data class Booking(
    val id: String = "",
    val bookingCode: String = "",
    val customerId: String = "",
    val packageId: String = "",
    val agencyId: String = "",
    val destinationId: String = "",
    val departureId: String = "",
    val travelDate: Timestamp? = null,
    val travelers: List<Traveler> = emptyList(),
    val totalAmount: Long = 0L,
    val commissionAmount: Long = 0L,
    val status: String = "held", // "held"|"confirmed"|"cancelled"|"completed"|"refunded"
    val paymentStatus: String = "pending", // "pending"|"paid"|"failed"|"refunded"
    val razorpayOrderId: String? = null,
    val razorpayPaymentId: String? = null,
    val holdExpiresAt: Timestamp? = null,
    val agencyPayoutStatus: String = "pending", // "pending"|"settled"
    val notes: String? = null,
    val createdAt: Timestamp? = null,
    // Transient helper fields for UI display
    val packageTitle: String = "",
    val destinationName: String = "",
    val agencyName: String = ""
)

data class Chat(
    val id: String = "",
    val customerId: String = "",
    val mode: String = "bot", // "bot"|"human"
    val status: String = "open", // "open"|"waiting_for_staff"|"closed"
    val assignedStaffId: String? = null,
    val lastMessage: String? = null,
    val lastMessageAt: Timestamp? = null
)

data class ChatMessage(
    val id: String = "",
    val sender: String = "customer", // "customer"|"bot"|"staff"
    val text: String = "",
    val createdAt: Timestamp? = null
)

data class CallbackRequest(
    val id: String = "",
    val customerId: String = "",
    val name: String = "",
    val phone: String = "",
    val topic: String = "",
    val status: String = "new", // "new"|"called"|"closed"
    val createdAt: Timestamp? = null
)

data class CompanySettings(
    val companyName: String = "Tour Manage",
    val supportPhones: List<String> = listOf("+91 98765 43210", "+91 98765 43211"),
    val supportEmail: String = "support@tourmanage.com",
    val supportHours: String = "Mon - Sun: 8:00 AM - 10:00 PM IST",
    val whatsappNumber: String = "+919876543210",
    val aboutText: String = "Tour Manage brings verified tour operators and local agencies from across India under one single marketplace.",
    val termsUrl: String = "https://tourmanage.com/terms"
)

data class WishlistItem(
    val packageId: String = "",
    val savedPrice: Long = 0L,
    val savedAt: Long = System.currentTimeMillis()
)

data class NotificationItem(
    val id: String = "",
    val customerId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "booking", // "booking" | "chat" | "wishlist_price" | "wishlist_seats" | "trip_reminder"
    val targetType: String = "booking", // "booking" | "package" | "chat"
    val targetId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class SearchHistoryItem(
    val id: String = "",
    val query: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class RecentlyViewedItem(
    val packageId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class NotificationPreferences(
    val bookingUpdates: Boolean = true,
    val chatMessages: Boolean = true,
    val priceAlerts: Boolean = true,
    val tripReminders: Boolean = true
)

data class BookingHoldResponse(
    val bookingId: String = "",
    val bookingCode: String = "",
    val mode: String = "demo",
    val totalAmount: Long = 0L,
    val razorpayOrderId: String? = null
)

