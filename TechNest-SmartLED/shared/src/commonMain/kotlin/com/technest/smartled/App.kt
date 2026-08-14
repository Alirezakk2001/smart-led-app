package com.technest.smartled

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.technest.smartled.core.domain.Screen
import com.technest.smartled.data.repository.DeviceRepositoryImpl
import com.technest.smartled.data.transport.MockTransport
import com.technest.smartled.feature.dashboard.DashboardScreen
import com.technest.smartled.feature.dashboard.DashboardViewModel
import com.technest.smartled.feature.devices.DevicesScreen
import com.technest.smartled.feature.devices.DevicesViewModel
import com.technest.smartled.feature.effects.EffectsScreen
import com.technest.smartled.feature.effects.EffectsViewModel
import com.technest.smartled.feature.settings.SettingsScreen
import com.technest.smartled.feature.settings.SettingsViewModel
import com.technest.smartled.feature.settings.ThemeMode
import com.technest.smartled.ui.theme.LedTheme

@Composable
fun App() {
    val transport = remember { MockTransport() }
    val repository = remember { DeviceRepositoryImpl(transport, transport) }
    val devicesViewModel = remember { DevicesViewModel(repository) }
    val dashboardViewModel = remember { DashboardViewModel(repository) }
    val effectsViewModel = remember { EffectsViewModel(repository) }
    val settingsViewModel = remember { SettingsViewModel(repository) }

    var themeMode by remember { mutableStateOf(ThemeMode.System) }

    LedTheme(themeMode = themeMode) {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentScreen is Screen.Dashboard,
                        onClick = { currentScreen = Screen.Dashboard },
                        icon = { Text("\u25C7") },
                        label = { Text("Dashboard") },
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Effects,
                        onClick = { currentScreen = Screen.Effects },
                        icon = { Text("\u2726") },
                        label = { Text("Effects") },
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Devices,
                        onClick = { currentScreen = Screen.Devices },
                        icon = { Text("\u25C9") },
                        label = { Text("Devices") },
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Settings,
                        onClick = { currentScreen = Screen.Settings },
                        icon = { Text("\u2699") },
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
                        viewModel = dashboardViewModel,
                        onNavigateToSettings = { currentScreen = Screen.Settings },
                    )
                    is Screen.Effects -> EffectsScreen(
                        viewModel = effectsViewModel,
                    )
                    is Screen.Devices -> DevicesScreen(devicesViewModel)
                    is Screen.Settings -> SettingsScreen(
                        viewModel = settingsViewModel,
                        onThemeChanged = { mode -> themeMode = mode },
                    )
                    is Screen.Setup -> {} // handled separately when needed
                }
            }
        }
    }
}
