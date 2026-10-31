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
import com.trading.app.presentation.game.GamesScreen
import com.trading.app.presentation.learn.LearnPracticeScreen
import com.trading.app.presentation.learn.flowbuilder.MoneyFlowBuilderScreen
import com.trading.app.presentation.learn.simulation.DynamicQuizScreen
import com.trading.app.presentation.news.NewsListScreen
import com.trading.app.presentation.onboarding.OnboardingScreen
import com.trading.app.presentation.orderentry.OrderEntryScreen
import com.trading.app.presentation.portfolio.PortfolioScreen
import com.trading.app.presentation.splash.SplashScreen
import com.trading.app.presentation.stockdetail.StockDetailScreen
import com.trading.app.presentation.watchlist.WatchlistScreen
import java.net.URLEncoder

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Welcome : Screen("welcome")
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object Watchlist : Screen("watchlist")
    data object Portfolio : Screen("portfolio")
    data object Games : Screen("games")
    data object NewsList : Screen("news_list")
    data object LearnPractice : Screen("learn_practice")
    data object MoneyFlowBuilder : Screen("money_flow_builder")
    data object DynamicQuiz : Screen("dynamic_quiz?questionId={questionId}") {
        fun createRoute(questionId: String? = null) = if (questionId != null) "dynamic_quiz?questionId=$questionId" else "dynamic_quiz"
    }
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
fun TradingNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Onboarding.route
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Onboarding.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            OnboardingScreen(
                onComplete = {
                    val prefs = context.getSharedPreferences("app_launch_prefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("has_seen_onboarding", true).apply()
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate(Screen.Onboarding.route) {
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
                },
                onLearnClick = {
                    navController.navigate(Screen.LearnPractice.route)
                },
                onInvestClick = {
                    navController.navigate(Screen.Watchlist.route) {
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
        composable(Screen.Games.route) {
            GamesScreen(
                onGameClick = { gameId ->
                    navController.navigate(Screen.GamePlayer.createRoute(gameId))
                },
                onBackClick = {
                    navController.popBackStack()
                }
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
        composable(Screen.LearnPractice.route) {
            LearnPracticeScreen(
                onBack = { navController.popBackStack() },
                onStockClick = { symbol -> navController.navigate(Screen.StockDetail.createRoute(symbol)) },
                onOpenQuizOfWeek = { navController.navigate(Screen.MoneyFlowBuilder.route) },
                onOpenDynamicQuiz = { questionId -> navController.navigate(Screen.DynamicQuiz.createRoute(questionId)) }
            )
        }
        composable(Screen.MoneyFlowBuilder.route) {
            MoneyFlowBuilderScreen(
                onExit = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.DynamicQuiz.route,
            arguments = listOf(
                navArgument("questionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            DynamicQuizScreen(
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
