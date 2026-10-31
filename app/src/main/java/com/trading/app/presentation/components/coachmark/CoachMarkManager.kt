package com.trading.app.presentation.components.coachmark

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext

class CoachMarkManager(context: Context) {
    private val prefs = context.getSharedPreferences("coach_marks_prefs", Context.MODE_PRIVATE)

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean("has_completed_home_onboarding", false)
        set(value) = prefs.edit().putBoolean("has_completed_home_onboarding", value).apply()

    fun reset() {
        prefs.edit().remove("has_completed_home_onboarding").apply()
    }
}

@Composable
fun rememberCoachMarkSteps(
    watchlistTabBounds: Rect? = null,
    tradingBalanceBounds: Rect? = null,
    learnSectionBounds: Rect? = null
): List<CoachMarkStep> {
    return remember(watchlistTabBounds, tradingBalanceBounds, learnSectionBounds) {
        listOf(
            // Step 1: Proof of concept & security notice
            CoachMarkStep(
                id = "poc_welcome_disclaimer",
                headline = "Welcome to Cash Cage",
                bodyBullets = listOf(
                    "Proof of concept prototype.",
                    "Simulated paper trading only.",
                    "No production security added.",
                    "Live market & trade APIs coming soon."
                ),
                targetBounds = tradingBalanceBounds,
                arrowOrientation = ArrowOrientation.LOOP_DOWN_RIGHT,
                buttonText = "Next"
            ),
            // Step 2: Live Market Watchlist & Sectors
            CoachMarkStep(
                id = "watchlist_sectors_step",
                headline = "Explore Market Sectors",
                bodyBullets = listOf(
                    "Real-time volatility & momentum.",
                    "Tap tabs to switch asset classes.",
                    "Track top gainers & losers."
                ),
                targetBounds = watchlistTabBounds,
                arrowOrientation = ArrowOrientation.LOOP_DOWN_RIGHT,
                buttonText = "Next"
            ),
            // Step 3: Interactive Simulations & Quizzes
            CoachMarkStep(
                id = "education_simulations_step",
                headline = "Learn & Practice",
                bodyBullets = listOf(
                    "Hands-on trading simulations.",
                    "Master risk rules & order depth.",
                    "Earn XP and build your streak."
                ),
                targetBounds = learnSectionBounds,
                arrowOrientation = ArrowOrientation.LOOP_DOWN_LEFT,
                buttonText = "Okay, Got It!"
            )
        )
    }
}
