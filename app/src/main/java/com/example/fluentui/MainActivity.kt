/*
 * ════════════════════════════════════════════════════════════════════════════════
 *  FluentUI Launcher — Main Activity
 * ════════════════════════════════════════════════════════════════════════════════
 */

package com.example.fluentui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.example.fluentui.data.SettingsManager
import com.example.fluentui.ui.components.AppScreen
import com.example.fluentui.ui.components.AppViewModel
import com.example.fluentui.ui.components.SetupWizardScreen
import com.example.fluentui.ui.theme.FluentUITheme
import com.example.fluentui.ui.utils.IconCache

class MainActivity : ComponentActivity() {

    private companion object {
        const val PREFS_NAME = "fluent_prefs"
        const val KEY_SETUP_COMPLETE = "setup_complete"
        const val KEY_LAUNCHER_LAYOUT = "launcher_layout"
        const val EXTRA_OPEN_ACTION_CENTER = "OPEN_ACTION_CENTER"
    }

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        configureEdgeToEdge()
        super.onCreate(savedInstanceState)
        configureWindowInsets()
        configureSoftInputMode()
        routeIncomingIntent(intent)

        setContent {
            FluentUITheme {
                LauncherRoot(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleHomeIntent(intent)
        routeIncomingIntent(intent)
        suppressActivityTransition()
    }

    override fun onResume() {
        super.onResume()
        suppressActivityTransition()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= 80) IconCache.clear()
    }

    @Composable
    private fun LauncherRoot(viewModel: AppViewModel) {
        val prefs = remember { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
        val context = LocalContext.current
        val settingsManager = remember { SettingsManager(context) }

        var isSetupComplete by remember {
            mutableStateOf(prefs.getBoolean(KEY_SETUP_COMPLETE, false))
        }

        when {
            !isSetupComplete -> SetupWizardScreen(
                viewModel = viewModel,
                onSetupComplete = {
                    prefs.edit().putBoolean(KEY_SETUP_COMPLETE, true).apply()
                    isSetupComplete = true
                }
            )
            else -> {
                AppScreen(viewModel = viewModel)
            }
        }
    }

    private fun configureEdgeToEdge() {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
    }

    private fun configureWindowInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val useLightIcons = !resources.configuration.isNightMode()
        controller?.isAppearanceLightStatusBars = useLightIcons
        controller?.isAppearanceLightNavigationBars = useLightIcons
    }

    private fun configureSoftInputMode() {
        @Suppress("DEPRECATION")
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun routeIncomingIntent(intent: Intent?) {
        intent ?: return
        if (intent.getBooleanExtra(EXTRA_OPEN_ACTION_CENTER, false)) {
            viewModel.toggleActionCenter(true)
        }
    }

    private fun handleHomeIntent(intent: Intent) {
        if (intent.hasCategory(Intent.CATEGORY_HOME) || intent.action == Intent.ACTION_MAIN) {
            viewModel.onHomePressed()
        }
    }

    private fun suppressActivityTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}

private fun Configuration.isNightMode(): Boolean =
    (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
