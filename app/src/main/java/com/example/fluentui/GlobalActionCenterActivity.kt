package com.example.fluentui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.example.fluentui.ui.theme.FluentUITheme
import kotlinx.coroutines.delay

class GlobalActionCenterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        overridePendingTransition(0, 0)

        setContent {
            FluentUITheme {
                var isVisible by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    delay(50)
                    isVisible = true
                }
            }
        }
    }

    override fun finishAfterTransition() {
        super.finishAfterTransition()
        overridePendingTransition(0, 0)
    }
}