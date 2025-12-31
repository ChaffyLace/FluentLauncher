package com.example.fluentui.theme.core

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader as AndroidShader

fun Modifier.micaBackground(
    blurRadius: Float = 30f,
    tint: Color = Color(0xFFF6F8FA),
    opacity: Float = 0.7f
): Modifier = this
    .graphicsLayer {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val re = AndroidRenderEffect.createBlurEffect(
                blurRadius,
                blurRadius,
                AndroidShader.TileMode.CLAMP
            )
            renderEffect = re.asComposeRenderEffect()
        }
    }
    .background(tint.copy(alpha = opacity))
