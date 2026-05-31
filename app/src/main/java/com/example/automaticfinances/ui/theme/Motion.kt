package com.example.automaticfinances.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

// ===========================================
// AutomaticFinances — Motion System
// Material 3 emphasized easing + standard durations.
// ===========================================

object MotionTokens {
    // Durations (ms)
    const val DurationShort = 150
    const val DurationMedium = 300
    const val DurationLong = 450
    const val DurationEnter = 350
    const val DurationExit = 250

    // Material 3 "emphasized" easing curves
    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val StandardEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    fun <T> emphasizedTween(durationMillis: Int = DurationMedium): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = EmphasizedEasing)

    fun <T> enterTween(): FiniteAnimationSpec<T> =
        tween(durationMillis = DurationEnter, easing = EmphasizedDecelerate)

    /** Gentle spring for content reveals / scale-in micro-interactions. */
    fun <T> gentleSpring(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)

    // === Expressive springs ===
    // Reproduce the feel of Material 3 Expressive's MotionScheme (which is `internal`
    // in material3 1.4.0) using the public `spring` API. "Spatial" springs animate
    // position/size and carry a little bounce; "effects" springs animate color/alpha
    // and are critically damped (no overshoot). Use these for the expressive look.

    /** Spatial — quick reactions (chips, small toggles). */
    fun <T> expressiveSpatialFast(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.9f, stiffness = 1400f)

    /** Spatial — default container/content motion, gently springy. */
    fun <T> expressiveSpatialDefault(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 380f)

    /** Spatial — large/slow hero transitions, more visible bounce. */
    fun <T> expressiveSpatialSlow(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 200f)

    /** Effects — color/alpha/elevation, no overshoot. */
    fun <T> expressiveEffectsDefault(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1f, stiffness = 1600f)
}
