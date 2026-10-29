package com.trading.app.presentation.game

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Alignment as RiveAlignment
import app.rive.runtime.kotlin.core.Fit
import com.trading.app.R
import com.trading.app.presentation.theme.DarkBackground

@Composable
fun GamePlayerScreen(
    gameId: String,
    onBack: () -> Unit
) {
    val game = remember(gameId) {
        AvailableMiniGames.find { it.id == gameId } ?: AvailableMiniGames.first()
    }
    var reloadKey by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    // Automatically orient the screen to match the game's native design (16:9 Landscape vs 9:16 Portrait)
    // so 100% of the game is visible across the entire screen with zero hidden/cropped areas
    DisposableEffect(game.id) {
        val activity = context as? Activity
        val window = activity?.window
        activity?.requestedOrientation = if (game.isLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Full-screen interactive Rive game canvas with autoBind = true so ViewModel-driven games
        // (Brawl Archery "Game Model" & Graveyard Shift "Game") bind and respond when clicking Start
        key(game.id, reloadKey) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    RiveAnimationView(ctx).apply {
                        multiTouchEnabled = true
                        try {
                            setRiveResource(
                                resId = game.riveResId,
                                artboardName = game.artboardName,
                                stateMachineName = game.stateMachineName,
                                autoplay = true,
                                autoBind = true,
                                fit = Fit.FILL,
                                alignment = RiveAlignment.CENTER
                            )
                        } catch (_: Exception) {
                            setRiveResource(
                                resId = game.riveResId,
                                autoplay = true,
                                autoBind = true,
                                fit = Fit.FILL,
                                alignment = RiveAlignment.CENTER
                            )
                        }
                    }
                },
                update = { view ->
                    view.fit = Fit.FILL
                    view.alignment = RiveAlignment.CENTER
                }
            )
        }

        // Top HUD Controls: Rive Back Button (back_button.riv) at top-left & Restart button at top-right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xA60D1117))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier
                        .size(28.dp)
                        .clickable(onClick = onBack),
                    factory = { ctx ->
                        RiveAnimationView(ctx).apply {
                            setRiveResource(
                                resId = R.raw.back_button,
                                stateMachineName = "State Machine 1",
                                autoplay = true,
                                autoBind = true,
                                fit = Fit.CONTAIN,
                                alignment = RiveAlignment.CENTER
                            )
                            setOnClickListener {
                                runCatching { fireState("State Machine 1", "Tap") }
                                onBack()
                            }
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xA60D1117))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                    .clickable { reloadKey++ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Restart Game",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
