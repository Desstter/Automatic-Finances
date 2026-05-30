package com.example.automaticfinances.utils

import androidx.compose.ui.graphics.Color

fun String.toComposeColor(): Color = try {
    Color(android.graphics.Color.parseColor(this))
} catch (e: Exception) {
    Color.Gray
}
