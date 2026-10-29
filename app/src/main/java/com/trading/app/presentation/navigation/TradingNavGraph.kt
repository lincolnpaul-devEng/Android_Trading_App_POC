package com.trading.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.trading.app.presentation.dashboard.DashboardScreen
import com.trading.app.presentation.game.GamePlayerScreen
import com.trading.app.presentation.news.NewsListScreen
import com.trading.app.presentation.orderentry.OrderEntryScreen
import com.trading.app.presentation.portfolio.PortfolioScreen
import com.trading.app.presentation.splash.SplashScreen
import com.trading.app.presentation.stockdetail.StockDetailScreen
import com.trading.app.presentation.watchlist.WatchlistScreen
import java.net.URLEncoder

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Dashboard : Screen("dashboard")
    data object Watchlist : Screen("watchlist")
    data object Portfolio : Screen("portfolio")
    data object NewsList : Screen("news_list")
    data object StockDetail : Screen("stock/{symbol}") {
        fun createRoute(symbol: String) = "stock/${URLEncoder.encode(symbol, "UTF-8")}"
    }
    data object OrderEntry : Screen("order/{symbol}") {
        fun createRoute(symbol: String) = "order/${URLEncoder.encode(symbol, "UTF-8")}"
    }
    data object GamePlayer : Screen("game/{gameId}") {
        fun createRoute(gameId: String) = "game/$gameId"
    }
}

@Composable
fun TradingNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onStockClick = { symbol -> navController.navigate(Screen.StockDetail.createRoute(symbol)) },
                onGameClick = { gameId -> navController.navigate(Screen.GamePlayer.createRoute(gameId)) },
                onProfileClick = {
                    navController.navigate(Screen.Portfolio.route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable(
            route = Screen.GamePlayer.route,
            arguments = listOf(navArgument("gameId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: "brave_knight"
            GamePlayerScreen(
                gameId = gameId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Watchlist.route) {
            WatchlistScreen(
                onStockClick = { symbol -> navController.navigate(Screen.StockDetail.createRoute(symbol)) },
                onViewAllNews = { navController.navigate(Screen.NewsList.route) }
            )
        }
        composable(Screen.NewsList.route) {
            NewsListScreen(
                onBack = { navController.popBackStack() },
                onStockClick = { symbol -> navController.navigate(Screen.StockDetail.createRoute(symbol)) }
            )
        }
        composable(Screen.Portfolio.route) {
            PortfolioScreen(
                onStockClick = { symbol -> navController.navigate(Screen.StockDetail.createRoute(symbol)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.StockDetail.route,
            arguments = listOf(navArgument("symbol") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "trading://stock/{symbol}" })
        ) {
            StockDetailScreen(
                onBack = { navController.popBackStack() },
                onTrade = { symbol -> navController.navigate(Screen.OrderEntry.createRoute(symbol)) }
            )
        }
        composable(
            route = Screen.OrderEntry.route,
            arguments = listOf(navArgument("symbol") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "trading://order/{symbol}" })
        ) {
            OrderEntryScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
