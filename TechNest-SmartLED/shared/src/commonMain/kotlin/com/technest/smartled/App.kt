package com.technest.smartled

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.technest.smartled.core.domain.Screen
import com.technest.smartled.feature.dashboard.DashboardScreen
import com.technest.smartled.feature.devices.DevicesScreen
import com.technest.smartled.feature.effects.EffectsScreen
import com.technest.smartled.feature.settings.SettingsScreen
import com.technest.smartled.ui.theme.LedTheme

@Composable
fun App() {
    LedTheme {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentScreen is Screen.Dashboard,
                        onClick = { currentScreen = Screen.Dashboard },
                        icon = { Text("◇") },
                        label = { Text("Dashboard") },
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Effects,
                        onClick = { currentScreen = Screen.Effects },
                        icon = { Text("✦") },
                        label = { Text("Effects") },
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Devices,
                        onClick = { currentScreen = Screen.Devices },
                        icon = { Text("◉") },
                        label = { Text("Devices") },
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Settings,
                        onClick = { currentScreen = Screen.Settings },
                        icon = { Text("⚙") },
                        label = { Text("Settings") },
                    )
                }
            },
        ) { paddingValues ->
            Surface(
                modifier = Modifier.padding(paddingValues),
                color = MaterialTheme.colorScheme.background,
            ) {
                when (currentScreen) {
                    is Screen.Dashboard -> DashboardScreen(
                        onNavigateToSettings = { currentScreen = Screen.Settings },
                    )
                    is Screen.Effects -> EffectsScreen()
                    is Screen.Devices -> DevicesScreen()
                    is Screen.Settings -> SettingsScreen()
                    is Screen.Setup -> {} // handled separately when needed
                }
            }
        }
    }
}
