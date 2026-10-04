package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.FirebaseProvider
import com.example.data.repository.AuthRepository
import com.example.data.repository.TourRepository
import com.example.ui.navigation.Screen
import com.example.ui.navigation.bottomNavItems
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.booking.BookingScreen
import com.example.ui.screens.calendar.CalendarScreen
import com.example.ui.screens.compare.CompareScreen
import com.example.ui.screens.history.ActivityHistoryScreen
import com.example.ui.screens.home.AgencyDetailScreen
import com.example.ui.screens.home.DestinationDetailScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.PackageDetailScreen
import com.example.ui.screens.notifications.NotificationScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.support.SupportScreen
import com.example.ui.screens.trips.MyTripsScreen
import com.example.ui.screens.wishlist.WishlistScreen
import com.example.ui.theme.TourManageTheme

class MainActivity : ComponentActivity() {

    private lateinit var authRepository: AuthRepository
    private lateinit var tourRepository: TourRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase manually with named Firestore DB as required
        FirebaseProvider.initialize(applicationContext)

        authRepository = AuthRepository(applicationContext)
        tourRepository = TourRepository(applicationContext, authRepository)

        setContent {
            TourManageTheme {
                TourManageApp(
                    tourRepository = tourRepository,
                    authRepository = authRepository
                )
            }
        }
    }
}

@Composable
fun TourManageApp(
    tourRepository: TourRepository,
    authRepository: AuthRepository
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isBottomBarVisible = currentRoute in listOf(
        Screen.Home.route,
        Screen.Compare.route,
        Screen.Calendar.route,
        Screen.Support.route,
        Screen.MyTrips.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar(
                    tonalElevation = 6.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 11.sp
                                )
                            },
                            modifier = Modifier.testTag("nav_${item.title.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // 1. HOME SCREEN
            composable(Screen.Home.route) {
                HomeScreen(
                    repository = tourRepository,
                    onNavigateToDestination = { destId ->
                        navController.navigate(Screen.DestinationDetail.createRoute(destId))
                    },
                    onNavigateToAgency = { agencyId ->
                        navController.navigate(Screen.AgencyDetail.createRoute(agencyId))
                    },
                    onNavigateToPackage = { pkgId ->
                        navController.navigate(Screen.PackageDetail.createRoute(pkgId))
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route)
                    },
                    onNavigateToCompare = {
                        navController.navigate(Screen.Compare.route)
                    },
                    onNavigateToWishlist = {
                        navController.navigate(Screen.Wishlist.route)
                    },
                    onNavigateToNotifications = {
                        navController.navigate(Screen.Notifications.route)
                    }
                )
            }

            // 2. DESTINATION DETAIL SCREEN
            composable(
                route = Screen.DestinationDetail.route,
                arguments = listOf(navArgument("destinationId") { type = NavType.StringType })
            ) { backStackEntry ->
                val destId = backStackEntry.arguments?.getString("destinationId") ?: ""
                DestinationDetailScreen(
                    destinationId = destId,
                    repository = tourRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPackage = { pkgId ->
                        navController.navigate(Screen.PackageDetail.createRoute(pkgId))
                    },
                    onNavigateToCompare = {
                        navController.navigate(Screen.Compare.route)
                    },
                    onNavigateToCalendar = {
                        navController.navigate(Screen.Calendar.route)
                    }
                )
            }

            // 3. AGENCY DETAIL SCREEN
            composable(
                route = Screen.AgencyDetail.route,
                arguments = listOf(navArgument("agencyId") { type = NavType.StringType })
            ) { backStackEntry ->
                val agencyId = backStackEntry.arguments?.getString("agencyId") ?: ""
                AgencyDetailScreen(
                    agencyId = agencyId,
                    repository = tourRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPackage = { pkgId ->
                        navController.navigate(Screen.PackageDetail.createRoute(pkgId))
                    },
                    onNavigateToBooking = { pkgId, depId ->
                        navController.navigate(Screen.Booking.createRoute(pkgId, depId))
                    },
                    onNavigateToSupport = {
                        navController.navigate(Screen.Support.route)
                    }
                )
            }

            // 4. PACKAGE DETAIL SCREEN
            composable(
                route = Screen.PackageDetail.route,
                arguments = listOf(navArgument("packageId") { type = NavType.StringType })
            ) { backStackEntry ->
                val pkgId = backStackEntry.arguments?.getString("packageId") ?: ""
                PackageDetailScreen(
                    packageId = pkgId,
                    repository = tourRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBooking = { id, depId ->
                        navController.navigate(Screen.Booking.createRoute(id, depId))
                    },
                    onNavigateToCompare = {
                        navController.navigate(Screen.Compare.route)
                    },
                    onNavigateToAgency = { agencyId ->
                        navController.navigate(Screen.AgencyDetail.createRoute(agencyId))
                    }
                )
            }

            // 5. COMPARE SCREEN
            composable(Screen.Compare.route) {
                CompareScreen(
                    repository = tourRepository,
                    onNavigateToBooking = { id, depId ->
                        navController.navigate(Screen.Booking.createRoute(id, depId))
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route)
                    }
                )
            }

            // 6. CALENDAR SCREEN
            composable(Screen.Calendar.route) {
                CalendarScreen(
                    repository = tourRepository,
                    onNavigateToBooking = { id, depId ->
                        navController.navigate(Screen.Booking.createRoute(id, depId))
                    }
                )
            }

            // 7. SUPPORT SCREEN
            composable(Screen.Support.route) {
                SupportScreen(
                    repository = tourRepository,
                    authRepo = authRepository,
                    onNavigateToAuth = {
                        navController.navigate(Screen.Auth.route)
                    }
                )
            }

            // 8. MY TRIPS SCREEN
            composable(Screen.MyTrips.route) {
                MyTripsScreen(
                    repository = tourRepository,
                    authRepo = authRepository,
                    onNavigateToAuth = {
                        navController.navigate(Screen.Auth.route)
                    },
                    onNavigateToChatWithBooking = { bookingCode ->
                        navController.navigate(Screen.Support.route)
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route)
                    }
                )
            }

            // 9. BOOKING SCREEN
            composable(
                route = Screen.Booking.route,
                arguments = listOf(
                    navArgument("packageId") { type = NavType.StringType },
                    navArgument("departureId") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val pkgId = backStackEntry.arguments?.getString("packageId") ?: ""
                val depId = backStackEntry.arguments?.getString("departureId") ?: ""
                BookingScreen(
                    packageId = pkgId,
                    initialDepartureId = depId,
                    repository = tourRepository,
                    authRepo = authRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMyTrips = {
                        navController.navigate(Screen.MyTrips.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onNavigateToChat = {
                        navController.navigate(Screen.Support.route)
                    },
                    onNavigateToAuth = {
                        navController.navigate(Screen.Auth.route)
                    }
                )
            }

            // 10. AUTH SCREEN
            composable(Screen.Auth.route) {
                AuthScreen(
                    authRepo = authRepository,
                    onAuthSuccess = {
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onContinueAsGuest = { navController.popBackStack() }
                )
            }

            // 11. PROFILE SCREEN
            composable(Screen.Profile.route) {
                ProfileScreen(
                    authRepo = authRepository,
                    tourRepo = tourRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAuth = {
                        navController.navigate(Screen.Auth.route)
                    },
                    onNavigateToWishlist = {
                        navController.navigate(Screen.Wishlist.route)
                    },
                    onNavigateToNotifications = {
                        navController.navigate(Screen.Notifications.route)
                    },
                    onNavigateToHistory = {
                        navController.navigate(Screen.History.createRoute(0))
                    }
                )
            }

            // 12. WISHLIST SCREEN
            composable(Screen.Wishlist.route) {
                WishlistScreen(
                    repository = tourRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPackage = { pkgId ->
                        navController.navigate(Screen.PackageDetail.createRoute(pkgId))
                    },
                    onNavigateToBooking = { pkgId, depId ->
                        navController.navigate(Screen.Booking.createRoute(pkgId, depId))
                    },
                    onNavigateToCompare = {
                        navController.navigate(Screen.Compare.route)
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route)
                    }
                )
            }

            // 13. NOTIFICATIONS SCREEN
            composable(Screen.Notifications.route) {
                NotificationScreen(
                    repository = tourRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenTarget = { targetType, targetId ->
                        when (targetType) {
                            "booking" -> navController.navigate(Screen.MyTrips.route)
                            "package" -> navController.navigate(Screen.PackageDetail.createRoute(targetId))
                            "chat" -> navController.navigate(Screen.Support.route)
                            else -> navController.navigate(Screen.Home.route)
                        }
                    }
                )
            }

            // 14. ACTIVITY HISTORY SCREEN
            composable(
                route = Screen.History.route,
                arguments = listOf(
                    navArgument("tab") {
                        type = NavType.IntType
                        defaultValue = 0
                    }
                )
            ) { backStackEntry ->
                val tab = backStackEntry.arguments?.getInt("tab") ?: 0
                ActivityHistoryScreen(
                    repository = tourRepository,
                    initialTab = tab,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPackage = { pkgId ->
                        navController.navigate(Screen.PackageDetail.createRoute(pkgId))
                    },
                    onNavigateToSearchQuery = { query ->
                        navController.navigate(Screen.Home.route)
                    },
                    onNavigateToChatWithBooking = { bookingCode ->
                        navController.navigate(Screen.Support.route)
                    }
                )
            }
        }
    }
}
