package com.trading.app.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.trading.app.notification.NotificationHelper
import com.trading.app.presentation.navigation.Screen
import com.trading.app.presentation.navigation.TradingNavGraph
import com.trading.app.presentation.splash.SplashScreen
import com.trading.app.presentation.theme.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Handled silently
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        notificationHelper.createNotificationChannels()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            TradingAppTheme {
                TradingAppRoot()
            }
        }
    }
}

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun TradingAppRoot() {
    var showSplash by rememberSaveable { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(
            onSplashComplete = {
                showSplash = false
            }
        )
    } else {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val bottomNavItems = listOf(
            BottomNavItem("Markets", Icons.Default.Dashboard, Screen.Dashboard.route),
            BottomNavItem("Watchlist", Icons.Default.Visibility, Screen.Watchlist.route),
            BottomNavItem("Portfolio", Icons.Default.AccountBalance, Screen.Portfolio.route),
        )

        val showBottomBar = currentRoute in bottomNavItems.map { it.route }

        Scaffold(
            containerColor = DarkBackground,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = DarkSurface,
                        contentColor = TextPrimary
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo(Screen.Dashboard.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(item.icon, contentDescription = item.label)
                                },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryBlue,
                                    selectedTextColor = PrimaryBlue,
                                    unselectedIconColor = TextTertiary,
                                    unselectedTextColor = TextTertiary,
                                    indicatorColor = PrimaryBlue.copy(alpha = 0.12f)
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            val isPortfolio = currentRoute == Screen.Portfolio.route
            val isGamePlayer = currentRoute == Screen.GamePlayer.route
            val contentPadding = when {
                isGamePlayer -> PaddingValues(0.dp)
                isPortfolio -> PaddingValues(bottom = padding.calculateBottomPadding())
                else -> padding
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .background(DarkBackground)
            ) {
                TradingNavGraph(navController)
            }
        }
    }
}
