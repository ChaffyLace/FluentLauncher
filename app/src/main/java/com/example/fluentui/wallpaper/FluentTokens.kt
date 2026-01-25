package com.example.fluentui.wallpaper

import androidx.compose.animation.core.FastOutSlowInEasing

object FluentTokens {
    object Mica {
        const val BLUR_RADIUS = 60f

        const val SURFACE_ALPHA_DARK = 0.50f
        const val SURFACE_ALPHA_LIGHT = 0.70f

        const val TINT_ALPHA_DARK = 0.50f
        const val TINT_ALPHA_LIGHT = 0.70f

        const val COLOR_BLEND_RATIO_DARK = 0.10f
        const val COLOR_BLEND_RATIO_LIGHT = 0.08f

        const val LUMINOSITY_THRESHOLD_DARK = 0.15
        const val LUMINOSITY_THRESHOLD_LIGHT = 0.85

        const val ANIMATION_DURATION = 800
        val ANIMATION_EASING = FastOutSlowInEasing
    }
}