package com.example.fluentui.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object FluentMotion {
    // === DURATIONS (Optimisé Mobile) ===
    const val DurationFaster = 83  // Micro-interactions (Clic)
    const val DurationFast = 167   // Sorties
    const val DurationNormal = 250 // Entrées
    const val DurationTouch = 300  // Scroll de page

    // === COURBES (EASINGS) ===
    val WindowsEnter = CubicBezierEasing(0f, 0f, 0f, 1f) // Fast Out, Slow In
    val WindowsExit = CubicBezierEasing(1f, 0f, 1f, 1f)  // Slow Out, Fast In
    val TouchEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)  // Tactile Hybrid

    // === PHYSIQUE (REBOND) ===
    fun <T> springFluent(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy, // Rebond sec et premium
        stiffness = Spring.StiffnessLow
    )

    // === HELPERS ===
    fun <T> enterSpec(duration: Int = DurationNormal): TweenSpec<T> =
        tween(durationMillis = duration, easing = WindowsEnter)

    fun <T> exitSpec(duration: Int = DurationFast): TweenSpec<T> =
        tween(durationMillis = duration, easing = WindowsExit)

    fun <T> pageScrollSpec(): TweenSpec<T> =
        tween(durationMillis = DurationTouch, easing = TouchEase)
}