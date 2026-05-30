package com.example.automaticfinances.ui.theme

import androidx.compose.ui.unit.dp

// ===========================================
// AutomaticFinances — Design System Tokens
// Spacing, elevation & sizing scale (8pt grid)
// Single source of truth for layout rhythm.
// ===========================================

/**
 * Spacing scale based on a 4/8pt grid. Use these instead of hardcoded dp
 * values so vertical/horizontal rhythm stays consistent across every screen.
 */
object Spacing {
    val none = 0.dp
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 48.dp

    /** Standard horizontal screen gutter. */
    val screen = 16.dp

    /** Default vertical gap between stacked sections/cards. */
    val section = 16.dp

    /** Default inner padding for cards. */
    val card = 20.dp
}

/**
 * Elevation tokens. The "Peso de Oro" identity favours flat, tonal surfaces
 * (elevation conveyed by surface color, not shadow) with subtle lift on press.
 */
object Elevations {
    val flat = 0.dp
    val raised = 1.dp
    val pressed = 3.dp
    val floating = 6.dp
}

/**
 * Common sizing tokens for touch targets, avatars and icons. Minimum touch
 * target is 48dp per Material accessibility guidance.
 */
object Sizes {
    val minTouchTarget = 48.dp
    val iconSm = 16.dp
    val iconMd = 20.dp
    val iconLg = 24.dp
    val avatarSm = 36.dp
    val avatarMd = 44.dp
    val avatarLg = 56.dp
}
