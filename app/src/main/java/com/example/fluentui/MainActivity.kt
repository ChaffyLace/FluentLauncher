package com.example.fluentui

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.example.fluentui.ui.components.AppScreen
import com.example.fluentui.ui.components.AppViewModel
import com.example.fluentui.ui.components.SetupWizardScreen
import com.example.fluentui.ui.theme.FluentUITheme
import com.example.fluentui.ui.utils.IconCache

/**
 * =================================================================================
 * CORE LAUNCHER ENTRY POINT
 * =================================================================================
 * Engineering Note: Manages system-level intents, memory trimming, window configuration,
 * and top-level navigation routing (OOBE vs Main Dashboard).
 * Configured strictly for an Edge-to-Edge experience with manual IME (keyboard) handling.
 */
class MainActivity : ComponentActivity() {

    // Global ViewModel scoped to the lifecycle of the entire Launcher Activity
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Enforce Edge-to-Edge display (Draw behind system bars)
        setupSystemBars()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 2. THE ULTIMATE IME FIX
        // Forces Android's WindowManager to resize the app window when the keyboard opens,
        // rather than pushing the entire UI off-screen (adjustPan).
        // This makes the Compose `.imePadding()` modifier actually work.
        @Suppress("DEPRECATION")
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setContent {
            FluentUITheme {
                // Initialize SharedPreferences to track the Out-Of-Box Experience (OOBE) state
                val prefs = getSharedPreferences("fluent_prefs", Context.MODE_PRIVATE)
                var isSetupComplete by remember {
                    mutableStateOf(prefs.getBoolean("setup_complete", false))
                }

                // Top-Level Router Strategy
                if (!isSetupComplete) {
                    SetupWizardScreen(
                        viewModel = viewModel,
                        onSetupComplete = {
                            // Persist the completion state so the wizard never shows again
                            prefs.edit().putBoolean("setup_complete", true).apply()
                            isSetupComplete = true
                        }
                    )
                } else {
                    // Launch the actual Home Screen / App Drawer
                    AppScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // =================================================================================
    // SYSTEM LIFECYCLE & PERFORMANCE MANAGEMENT
    // =================================================================================

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Aggressively release heavy bitmap resources when the Android OS is under memory pressure.
        // Critical for a Launcher to maintain high performance and avoid OOM (Out Of Memory) kills.
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            IconCache.clear()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Intercept physical or nav-bar home button presses to reset the launcher state
        // (e.g., closing the app drawer or search overlay).
        if (intent.hasCategory(Intent.CATEGORY_HOME) || intent.action == Intent.ACTION_MAIN) {
            viewModel.onHomePressed()
        }
        cancelTransition()
    }

    override fun onResume() {
        super.onResume()
        // Ensure no default Android activity transition animations play when returning to home.
        // A launcher must appear instantly.
        cancelTransition()
    }

    /**
     * Disables the default Android transition animations.
     * This is mandatory for a custom Launcher to ensure it mimics native OS behavior.
     */
    private fun cancelTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34+
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    /**
     * Makes the activity layout full-screen (behind status and navigation bars)
     * and sets the system bar backgrounds to transparent for the Mica effect.
     */
    private fun setupSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        // This ensures icons (clock, battery) remain visible against the wallpaper.
        // In a fully dynamic setup, these should react to the wallpaper's luminance.
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }
}