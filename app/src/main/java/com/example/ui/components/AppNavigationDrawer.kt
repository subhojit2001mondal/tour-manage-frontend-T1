package com.example.ui.components

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.data.models.CustomerProfile
import com.example.data.repository.AppUser
import com.example.data.repository.ThemeMode
import com.example.ui.navigation.Screen
import com.example.ui.theme.TourGoldDark
import com.example.ui.theme.TourGoldLight
import com.example.ui.theme.TourNavy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class DrawerMenuItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0,
    val hasDotBadge: Boolean = false,
    val testTag: String
)

@Composable
fun AppNavigationDrawer(
    drawerState: DrawerState,
    scope: CoroutineScope,
    currentRoute: String?,
    currentUser: AppUser?,
    customerProfile: CustomerProfile?,
    wishlistCount: Int,
    unreadNotificationsCount: Int,
    hasUpdateAvailable: Boolean = false,
    themeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTabletOrLandscape = configuration.screenWidthDp >= 720 ||
        (configuration.screenWidthDp >= 600 && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)

    // Handle back button to close drawer if open
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    val menuItems = listOf(
        DrawerMenuItem(
            route = Screen.Home.route,
            title = "Home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            testTag = "drawer_item_home"
        ),
        DrawerMenuItem(
            route = Screen.Compare.route,
            title = "Comparison",
            selectedIcon = Icons.AutoMirrored.Filled.CompareArrows,
            unselectedIcon = Icons.AutoMirrored.Filled.CompareArrows,
            testTag = "drawer_item_comparison"
        ),
        DrawerMenuItem(
            route = Screen.Wishlist.route,
            title = "Wishlist",
            selectedIcon = Icons.Filled.Favorite,
            unselectedIcon = Icons.Outlined.FavoriteBorder,
            badgeCount = wishlistCount,
            testTag = "drawer_item_wishlist"
        ),
        DrawerMenuItem(
            route = Screen.MyTrips.route,
            title = "My Trips",
            selectedIcon = Icons.Filled.Luggage,
            unselectedIcon = Icons.Outlined.Luggage,
            testTag = "drawer_item_my_trips"
        ),
        DrawerMenuItem(
            route = Screen.Notifications.route,
            title = "Notifications",
            selectedIcon = Icons.Filled.Notifications,
            unselectedIcon = Icons.Outlined.Notifications,
            badgeCount = unreadNotificationsCount,
            testTag = "drawer_item_notifications"
        ),
        DrawerMenuItem(
            route = Screen.History.createRoute(0),
            title = "Activity & History",
            selectedIcon = Icons.Filled.History,
            unselectedIcon = Icons.Outlined.History,
            testTag = "drawer_item_history"
        ),
        DrawerMenuItem(
            route = Screen.Support.route,
            title = "Support",
            selectedIcon = Icons.Filled.SupportAgent,
            unselectedIcon = Icons.Outlined.SupportAgent,
            testTag = "drawer_item_support"
        ),
        DrawerMenuItem(
            route = Screen.Update.route,
            title = "Check for updates",
            selectedIcon = Icons.Filled.SystemUpdate,
            unselectedIcon = Icons.Outlined.SystemUpdate,
            hasDotBadge = hasUpdateAvailable,
            testTag = "drawer_item_update"
        )
    )

    val drawerContentComposable: @Composable () -> Unit = {
        DrawerSheetContent(
            currentUser = currentUser,
            customerProfile = customerProfile,
            currentRoute = currentRoute,
            menuItems = menuItems,
            themeMode = themeMode,
            onThemeModeChanged = onThemeModeChanged,
            onSelectMenuItem = { route ->
                scope.launch { drawerState.close() }
                onNavigateToRoute(route)
            },
            onSignInClick = {
                scope.launch { drawerState.close() }
                onSignIn()
            },
            onSignOutClick = {
                scope.launch { drawerState.close() }
                onSignOut()
            }
        )
    }

    if (isTabletOrLandscape) {
        // Permanent side drawer for tablets & landscape
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .fillMaxHeight(),
                    drawerContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                ) {
                    drawerContentComposable()
                }
            },
            content = content
        )
    } else {
        // Modal drawer for phones: 80% width (max 320dp), semi-transparent 85-90% surface, rounded right corners, soft border, dark scrim
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            scrimColor = DrawerDefaults.scrimColor,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier
                        .fillMaxWidth(0.80f)
                        .widthIn(max = 320.dp)
                        .fillMaxHeight()
                        .border(
                            BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                        ),
                    drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    drawerTonalElevation = 4.dp
                ) {
                    drawerContentComposable()
                }
            },
            content = content
        )
    }
}

@Composable
private fun DrawerSheetContent(
    currentUser: AppUser?,
    customerProfile: CustomerProfile?,
    currentRoute: String?,
    menuItems: List<DrawerMenuItem>,
    themeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onSelectMenuItem: (String) -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 16.dp)
    ) {
        // 1. DRAWER HEADER (Avatar, Name, Email, DEMO chip or Guest)
        DrawerHeader(
            currentUser = currentUser,
            customerProfile = customerProfile,
            onSignInClick = onSignInClick
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )

        // 2. MENU ITEMS
        menuItems.forEach { item ->
            val isSelected = currentRoute == item.route ||
                (item.route.startsWith("history") && currentRoute?.startsWith("history") == true) ||
                (item.route == Screen.Home.route && (currentRoute == null || currentRoute == Screen.Home.route))

            NavigationDrawerItem(
                label = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            if (item.hasDotBadge) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(TourGoldDark)
                                )
                            }
                        }
                        if (item.badgeCount > 0) {
                            Badge(
                                containerColor = if (item.route == Screen.Wishlist.route) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = item.badgeCount.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                selected = isSelected,
                onClick = { onSelectMenuItem(item.route) },
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .testTag(item.testTag),
                shape = RoundedCornerShape(12.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedContainerColor = Color.Transparent,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        // 3. SIGN IN / SIGN OUT ITEM
        val isAuthScreen = currentRoute == Screen.Auth.route
        if (currentUser != null) {
            NavigationDrawerItem(
                label = {
                    Text(
                        text = "Sign Out",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign Out",
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                selected = false,
                onClick = onSignOutClick,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .testTag("drawer_item_sign_out"),
                shape = RoundedCornerShape(12.dp)
            )
        } else {
            NavigationDrawerItem(
                label = {
                    Text(
                        text = "Sign In / Sign Up",
                        fontWeight = if (isAuthScreen) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Login,
                        contentDescription = "Sign In"
                    )
                },
                selected = isAuthScreen,
                onClick = onSignInClick,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .testTag("drawer_item_sign_in"),
                shape = RoundedCornerShape(12.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )

        // 4. DAY / NIGHT MODE CONTROL (System, Light, Dark)
        DayNightModeControl(
            currentMode = themeMode,
            onModeSelected = onThemeModeChanged
        )

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(16.dp))

        // 5. APP VERSION AT BOTTOM
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tour Manage v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "All India Tour Marketplace",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DrawerHeader(
    currentUser: AppUser?,
    customerProfile: CustomerProfile?,
    onSignInClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("drawer_header")
    ) {
        if (currentUser != null) {
            val displayName = customerProfile?.name?.ifBlank { currentUser.displayName ?: "Traveler" }
                ?: currentUser.displayName ?: "Traveler"
            val email = currentUser.email ?: "traveler@tourmanage.com"
            val initials = displayName.split("\\s+".toRegex())
                .filter { it.isNotEmpty() }
                .take(2)
                .map { it.first().uppercase() }
                .joinToString("")
                .ifBlank { "T" }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.size(52.dp),
                    shadowElevation = 2.dp
                ) {
                    if (!currentUser.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = currentUser.photoUrl,
                            contentDescription = displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = initials,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Mode Chip
                        if (currentUser.isDemo) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = TourGoldLight,
                                contentColor = TourGoldDark,
                                border = BorderStroke(1.dp, TourGoldDark.copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("demo_chip_header")
                            ) {
                                Text(
                                    text = "DEMO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDCFCE7),
                                contentColor = Color(0xFF15803D),
                                border = BorderStroke(1.dp, Color(0xFF86EFAC))
                            ) {
                                Text(
                                    text = "VERIFIED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (currentUser.isDemo) "Logged in as $email" else email,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            // Guest Traveler Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = "Guest",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Guest Traveler",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Browsing mode",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onSignInClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drawer_sign_in_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Login,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sign in to your account", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun DayNightModeControl(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("day_night_mode_control")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = when (currentMode) {
                    ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
                    ThemeMode.LIGHT -> Icons.Outlined.LightMode
                    ThemeMode.DARK -> Icons.Outlined.DarkMode
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Day / Night Mode",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Segmented options: System | Light | Dark
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ThemeOptionButton(
                label = "System",
                icon = Icons.Outlined.BrightnessAuto,
                isSelected = currentMode == ThemeMode.SYSTEM,
                onClick = { onModeSelected(ThemeMode.SYSTEM) },
                modifier = Modifier.weight(1f).testTag("theme_mode_system")
            )
            ThemeOptionButton(
                label = "Light",
                icon = Icons.Outlined.LightMode,
                isSelected = currentMode == ThemeMode.LIGHT,
                onClick = { onModeSelected(ThemeMode.LIGHT) },
                modifier = Modifier.weight(1f).testTag("theme_mode_light")
            )
            ThemeOptionButton(
                label = "Dark",
                icon = Icons.Outlined.DarkMode,
                isSelected = currentMode == ThemeMode.DARK,
                onClick = { onModeSelected(ThemeMode.DARK) },
                modifier = Modifier.weight(1f).testTag("theme_mode_dark")
            )
        }
    }
}

@Composable
private fun ThemeOptionButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
