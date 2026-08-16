package com.technest.smartled.ui.ambient

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.technest.smartled.ui.theme.DynamicAccent

/**
 * CompositionLocal that provides the current [DynamicAccent] derived from LED state.
 *
 * Components can read this to adapt their appearance:
 * - Dashboard, Effects → use dynamic accent for ambient UI
 * - Settings, Hardware Config → use default (calm) behavior
 *
 * Usage:
 * ```kotlin
 * val accent = LocalDynamicAccent.current
 * Surface(color = accent.accentSoft) { ... }
 * ```
 */
val LocalDynamicAccent = staticCompositionLocalOf { DynamicAccent() }

/**
 * Wraps content with a [DynamicAccent] provider.
 *
 * @param accent The dynamic accent palette to provide.
 * @param content The composable content that will read the accent.
 */
@Composable
fun DynamicAccentProvider(
    accent: DynamicAccent,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDynamicAccent provides accent) {
        content()
    }
}
