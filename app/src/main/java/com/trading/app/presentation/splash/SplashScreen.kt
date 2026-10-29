package com.trading.app.presentation.splash

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Fit
import app.rive.runtime.kotlin.core.Alignment as RiveAlignment
import com.trading.app.R
import com.trading.app.presentation.theme.DarkBackground
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    val context = LocalContext.current

    // Hide system navigation bar and status bar for a truly immersive full-screen splash
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            val window = (context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(2800) // Full startup animation duration before transition
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .clickable { onSplashComplete() }
    ) {
        // Real full-screen startup splash: fills 100% of the mobile screen edge-to-edge
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                RiveAnimationView(ctx).apply {
                    setRiveResource(
                        resId = R.raw.splash_screen,
                        fit = Fit.COVER,
                        alignment = RiveAlignment.CENTER,
                        autoplay = true
                    )
                }
            }
        )
    }
}
