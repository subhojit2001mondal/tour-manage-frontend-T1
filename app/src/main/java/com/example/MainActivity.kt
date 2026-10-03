package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.example.ui.screens.home.DestinationDetailScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.PackageDetailScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.support.SupportScreen
import com.example.ui.screens.trips.MyTripsScreen
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
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. HOME SCREEN
            composable(Screen.Home.route) {
                HomeScreen(
                    repository = tourRepository,
                    onNavigateToDestination = { destId ->
                        navController.navigate(Screen.DestinationDetail.createRoute(destId))
                    },
                    onNavigateToPackage = { pkgId ->
                        navController.navigate(Screen.PackageDetail.createRoute(pkgId))
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route)
                    },
                    onNavigateToCompare = {
                        navController.navigate(Screen.Compare.route)
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

            // 3. PACKAGE DETAIL SCREEN
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
                    }
                )
            }

            // 4. COMPARE SCREEN
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

            // 5. CALENDAR SCREEN
            composable(Screen.Calendar.route) {
                CalendarScreen(
                    repository = tourRepository,
                    onNavigateToBooking = { id, depId ->
                        navController.navigate(Screen.Booking.createRoute(id, depId))
                    }
                )
            }

            // 6. SUPPORT SCREEN
            composable(Screen.Support.route) {
                SupportScreen(
                    repository = tourRepository,
                    authRepo = authRepository,
                    onNavigateToAuth = {
                        navController.navigate(Screen.Auth.route)
                    }
                )
            }

            // 7. MY TRIPS SCREEN
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

            // 8. BOOKING SCREEN
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

            // 9. AUTH SCREEN
            composable(Screen.Auth.route) {
                AuthScreen(
                    authRepo = authRepository,
                    onAuthSuccess = { navController.popBackStack() },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 10. PROFILE SCREEN
            composable(Screen.Profile.route) {
                ProfileScreen(
                    authRepo = authRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAuth = {
                        navController.navigate(Screen.Auth.route)
                    }
                )
            }
        }
    }
}
