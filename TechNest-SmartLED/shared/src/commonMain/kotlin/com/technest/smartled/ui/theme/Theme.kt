package com.technest.smartled.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.technest.smartled.feature.settings.ThemeMode

private val DarkColorScheme = darkColorScheme(
    background = LedColors.DarkBackground,
    surface = LedColors.DarkSurface,
    surfaceVariant = LedColors.DarkSurfaceElevated,
    onBackground = LedColors.DarkTextPrimary,
    onSurface = LedColors.DarkTextPrimary,
    onSurfaceVariant = LedColors.DarkTextSecondary,
    primary = LedColors.AccentCyan,
    secondary = LedColors.AccentPurple,
    tertiary = LedColors.AccentBlue,
    outline = LedColors.DarkTextDisabled,
)

private val LightColorScheme = lightColorScheme(
    background = LedColors.LightBackground,
    surface = LedColors.LightSurface,
    surfaceVariant = LedColors.LightSurface,
    onBackground = LedColors.LightTextPrimary,
    onSurface = LedColors.LightTextPrimary,
    onSurfaceVariant = LedColors.LightTextSecondary,
    primary = LedColors.AccentBlue,
    secondary = LedColors.AccentPurple,
    tertiary = LedColors.AccentCyan,
    outline = LedColors.LightTextDisabled,
)

@Composable
fun LedTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LedTypography,
        content = content,
    )
}
