package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Compare : Screen("compare")
    data object Calendar : Screen("calendar")
    data object Support : Screen("support")
    data object MyTrips : Screen("my_trips")
    data object Profile : Screen("profile")
    data object Auth : Screen("auth")
    data object DestinationDetail : Screen("destination/{destinationId}") {
        fun createRoute(destinationId: String) = "destination/$destinationId"
    }
    data object PackageDetail : Screen("package/{packageId}") {
        fun createRoute(packageId: String) = "package/$packageId"
    }
    data object Booking : Screen("booking/{packageId}?departureId={departureId}") {
        fun createRoute(packageId: String, departureId: String = "") = "booking/$packageId?departureId=$departureId"
    }
}

data class BottomNavItem(
    val title: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

val bottomNavItems = listOf(
    BottomNavItem(
        title = "Home",
        route = Screen.Home.route,
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore
    ),
    BottomNavItem(
        title = "Compare",
        route = Screen.Compare.route,
        selectedIcon = Icons.AutoMirrored.Filled.CompareArrows,
        unselectedIcon = Icons.AutoMirrored.Outlined.CompareArrows
    ),
    BottomNavItem(
        title = "Calendar",
        route = Screen.Calendar.route,
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    ),
    BottomNavItem(
        title = "Support",
        route = Screen.Support.route,
        selectedIcon = Icons.Filled.Headphones,
        unselectedIcon = Icons.Outlined.Headphones
    ),
    BottomNavItem(
        title = "My Trips",
        route = Screen.MyTrips.route,
        selectedIcon = Icons.Filled.Luggage,
        unselectedIcon = Icons.Outlined.Luggage
    )
)
