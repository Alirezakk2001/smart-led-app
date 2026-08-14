package com.technest.smartled.ui.theme

import androidx.compose.ui.graphics.Color

object LedColors {
    // Dark theme
    val DarkBackground = Color(0xFF0B0D10)
    val DarkSurface = Color(0xFF12151A)
    val DarkSurfaceElevated = Color(0xFF181C22)
    val DarkTextPrimary = Color(0xFFF5F7FA)
    val DarkTextSecondary = Color(0xFFA8AFBA)
    val DarkTextDisabled = Color(0xFF626974)

    // Light theme
    val LightBackground = Color(0xFFF6F7F9)
    val LightSurface = Color(0xFFFFFFFF)
    val LightTextPrimary = Color(0xFF15181D)
    val LightTextSecondary = Color(0xFF6B7280)
    val LightTextDisabled = Color(0xFF9CA3AF)

    // Accent palette (dynamic, derived from LED state)
    val AccentRed = Color(0xFFFF4444)
    val AccentGreen = Color(0xFF44FF44)
    val AccentBlue = Color(0xFF4444FF)
    val AccentPurple = Color(0xFFFF44FF)
    val AccentCyan = Color(0xFF44FFFF)
    val AccentYellow = Color(0xFFFFFF44)
    val AccentOrange = Color(0xFFFF8844)

    // Status
    val StatusConnected = Color(0xFF44FF44)
    val StatusDisconnected = Color(0xFFFF4444)
    val StatusBusy = Color(0xFFFFAA00)
}
