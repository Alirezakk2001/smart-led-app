package com.technest.smartled.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onThemeChanged: (ThemeMode) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Appearance Section
        SectionHeader(title = "Appearance")
        ThemeSelector(
            currentMode = state.themeMode,
            onModeSelected = { mode ->
                viewModel.setThemeMode(mode)
                onThemeChanged(mode)
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Device Section
        SectionHeader(title = "Device")
        DeviceInfoSection(
            isConnected = state.isConnected,
            deviceName = state.deviceName,
            deviceAddress = state.deviceAddress,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Hardware Section
        SectionHeader(title = "Hardware Configuration")
        HardwareConfigSection(
            lineCount = state.lineCount,
            ledsPerLine = state.ledsPerLine,
            isConnected = state.isConnected,
            isApplying = state.isApplyingConfig,
            onLineCountChanged = { viewModel.setLineCount(it) },
            onLedsForLineChanged = { index, count -> viewModel.setLedsForLine(index, count) },
            onApply = { viewModel.applyConfiguration() },
        )

        // Status messages
        state.configAppliedMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        state.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About Section
        SectionHeader(title = "About")
        AboutSection(
            firmwareVersion = state.firmwareVersion,
            protocolVersion = state.protocolVersion,
            hardwareVersion = state.hardwareVersion,
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun ThemeSelector(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
) {
    Column {
        ThemeMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = currentMode == mode,
                    onClick = { onModeSelected(mode) },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (mode) {
                        ThemeMode.System -> "System"
                        ThemeMode.Dark -> "Dark"
                        ThemeMode.Light -> "Light"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun DeviceInfoSection(
    isConnected: Boolean,
    deviceName: String,
    deviceAddress: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            InfoRow(label = "Status", value = if (isConnected) "Connected" else "Disconnected")
            if (isConnected) {
                InfoRow(label = "Name", value = deviceName)
                InfoRow(label = "Address", value = deviceAddress)
            }
        }
    }
}

@Composable
private fun HardwareConfigSection(
    lineCount: Int,
    ledsPerLine: List<Int>,
    isConnected: Boolean,
    isApplying: Boolean,
    onLineCountChanged: (Int) -> Unit,
    onLedsForLineChanged: (Int, Int) -> Unit,
    onApply: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Line Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Number of lines",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(
                        onClick = { onLineCountChanged(lineCount - 1) },
                        enabled = lineCount > 1 && isConnected,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Text("-", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "$lineCount",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    FilledIconButton(
                        onClick = { onLineCountChanged(lineCount + 1) },
                        enabled = lineCount < 8 && isConnected,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Text("+", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // LEDs per line
            ledsPerLine.forEachIndexed { index, ledCount ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Line ${index + 1} LEDs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledIconButton(
                            onClick = { onLedsForLineChanged(index, ledCount - 10) },
                            enabled = ledCount > 10 && isConnected,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Text("-", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "$ledCount",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                        FilledIconButton(
                            onClick = { onLedsForLineChanged(index, ledCount + 10) },
                            enabled = ledCount < 512 && isConnected,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Text("+", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onApply,
                enabled = isConnected && !isApplying,
                modifier = Modifier.align(Alignment.End),
            ) {
                if (isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isApplying) "Applying..." else "Apply Configuration")
            }
        }
    }
}

@Composable
private fun AboutSection(
    firmwareVersion: String,
    protocolVersion: Int,
    hardwareVersion: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            InfoRow(label = "App Version", value = "1.0.0")
            InfoRow(label = "Firmware Version", value = firmwareVersion)
            InfoRow(label = "Hardware Version", value = hardwareVersion)
            InfoRow(label = "Protocol Version", value = protocolVersion.toString())
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
