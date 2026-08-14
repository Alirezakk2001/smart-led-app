package com.technest.smartled.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.technest.smartled.core.model.Brightness
import com.technest.smartled.core.model.EffectId
import com.technest.smartled.core.model.LedLineState
import com.technest.smartled.core.model.RgbColor
import com.technest.smartled.ui.theme.LedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    // Derive ambient color from the first active line's LED color
    val ambientColor = remember(state.lines, state.power) {
        val activeLine = state.lines.firstOrNull { it.enabled && state.power }
        if (activeLine != null) {
            Color(
                activeLine.color.red / 255f,
                activeLine.color.green / 255f,
                activeLine.color.blue / 255f,
                0.03f,
            )
        } else {
            Color(0f, 0f, 0f, 0f)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Ambient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ambientColor),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Connection status bar with settings button
            ConnectionStatusBar(
                isConnected = state.isConnected,
                deviceName = state.deviceName,
                deviceAddress = state.deviceAddress,
                onNavigateToSettings = onNavigateToSettings,
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.lines.isEmpty() && !state.isConnected) {
                // Disconnected empty state
                DisconnectedHint(onNavigateToSettings = onNavigateToSettings)
            } else {
                // Power & Brightness section
                PowerBrightnessSection(
                    power = state.power,
                    brightness = state.brightness,
                    isConnected = state.isConnected,
                    isSendingCommand = state.isSendingCommand,
                    onTogglePower = { viewModel.togglePower() },
                    onBrightnessChange = { viewModel.setBrightness(it) },
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Line selector
                if (state.lineCount > 1) {
                    LineSelector(
                        lineCount = state.lineCount,
                        selectedLineIndex = state.selectedLineIndex,
                        lines = state.lines,
                        onLineSelected = { viewModel.selectLine(it) },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // LED Preview
                LedPreview(
                    lines = state.lines,
                    power = state.power,
                    brightness = state.brightness,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Line controls
                if (state.selectedLineIndex != null && state.lines.isNotEmpty()) {
                    val line = state.lines.getOrNull(state.selectedLineIndex)
                    if (line != null) {
                        LineControlsSection(
                            line = line,
                            lineIndex = state.selectedLineIndex,
                            isConnected = state.isConnected,
                            isSendingCommand = state.isSendingCommand,
                            onColorChange = { color -> viewModel.setLineColor(state.selectedLineIndex, color) },
                            onEffectChange = { effect -> viewModel.setLineEffect(state.selectedLineIndex, effect) },
                            onSpeedChange = { speed -> viewModel.setLineSpeed(state.selectedLineIndex, speed) },
                            onBrightnessChange = { brightness -> viewModel.setLineBrightness(state.selectedLineIndex, brightness) },
                        )
                    }
                } else if (state.selectedLineIndex == null && state.lines.isNotEmpty()) {
                    // "All" selected - show global controls
                    AllLinesSection(
                        lines = state.lines,
                        isConnected = state.isConnected,
                        isSendingCommand = state.isSendingCommand,
                        onColorChange = { color ->
                            state.lines.forEach { line ->
                                viewModel.setLineColor(line.index, color)
                            }
                        },
                        onEffectChange = { effect ->
                            state.lines.forEach { line ->
                                viewModel.setLineEffect(line.index, effect)
                            }
                        },
                        onSpeedChange = { speed ->
                            state.lines.forEach { line ->
                                viewModel.setLineSpeed(line.index, speed)
                            }
                        },
                        onBrightnessChange = { brightness ->
                            state.lines.forEach { line ->
                                viewModel.setLineBrightness(line.index, brightness)
                            }
                        },
                    )
                }
            }

            // Error message
            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = state.errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            // Bottom spacer for nav bar
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun DisconnectedHint(onNavigateToSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "No Device Connected",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Connect to a device from the Devices screen to control your LED lights.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateToSettings) {
                Text("Go to Devices")
            }
        }
    }
}

@Composable
private fun ConnectionStatusBar(
    isConnected: Boolean,
    deviceName: String,
    deviceAddress: String,
    onNavigateToSettings: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (isConnected) LedColors.StatusConnected
                        else LedColors.StatusDisconnected
                    ),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (isConnected) {
                    Text(
                        text = deviceName.ifEmpty { "Connected" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = deviceAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "Disconnected",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Connect from Devices screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Settings icon button
            IconButton(onClick = onNavigateToSettings) {
                Text(
                    text = "\u2699",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PowerBrightnessSection(
    power: Boolean,
    brightness: Brightness,
    isConnected: Boolean,
    isSendingCommand: Boolean,
    onTogglePower: () -> Unit,
    onBrightnessChange: (Brightness) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Power toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Power",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = power,
                    onCheckedChange = { onTogglePower() },
                    enabled = isConnected && !isSendingCommand,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Brightness slider
            Text(
                text = "Brightness: ${brightness.toPercent()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = brightness.value.toFloat(),
                onValueChange = { onBrightnessChange(Brightness(it.toInt())) },
                valueRange = 0f..255f,
                steps = 50,
                enabled = isConnected && !isSendingCommand,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LineSelector(
    lineCount: Int,
    selectedLineIndex: Int?,
    lines: List<LedLineState>,
    onLineSelected: (Int?) -> Unit,
) {
    Column {
        Text(
            text = "Lines",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                LineChip(
                    label = "All",
                    isSelected = selectedLineIndex == null,
                    color = null,
                    onClick = { onLineSelected(null) },
                )
            }
            items(lines) { line ->
                LineChip(
                    label = "${line.index + 1}",
                    isSelected = selectedLineIndex == line.index,
                    color = line.color,
                    onClick = { onLineSelected(line.index) },
                )
            }
        }
    }
}

@Composable
private fun LineChip(
    label: String,
    isSelected: Boolean,
    color: RgbColor?,
    onClick: () -> Unit,
) {
    val chipColor = if (color != null) {
        Color(color.red / 255f, color.green / 255f, color.blue / 255f)
    } else null

    ElevatedFilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (chipColor != null) {
            {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(chipColor),
                )
            }
        } else null,
    )
}

@Composable
private fun LedPreview(
    lines: List<LedLineState>,
    power: Boolean,
    brightness: Brightness,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        if (lines.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No lines configured",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                lines.forEach { line ->
                    val alpha = if (power && line.enabled) {
                        (brightness.value.toFloat() / 255f) * 0.9f + 0.1f
                    } else 0.05f

                    val ledColor = Color(
                        line.color.red / 255f,
                        line.color.green / 255f,
                        line.color.blue / 255f,
                        alpha,
                    )

                    val glowColor = Color(
                        line.color.red / 255f,
                        line.color.green / 255f,
                        line.color.blue / 255f,
                        alpha * 0.3f,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = "${line.index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.width(16.dp),
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(glowColor)
                                .border(
                                    width = 1.dp,
                                    color = ledColor.copy(alpha = alpha * 0.5f),
                                    shape = RoundedCornerShape(4.dp),
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LineControlsSection(
    line: LedLineState,
    lineIndex: Int,
    isConnected: Boolean,
    isSendingCommand: Boolean,
    onColorChange: (RgbColor) -> Unit,
    onEffectChange: (EffectId) -> Unit,
    onSpeedChange: (Int) -> Unit,
    onBrightnessChange: (Brightness) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Line ${lineIndex + 1}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Color selector
            Text(
                text = "Color",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            ColorSelector(
                selectedColor = line.color,
                enabled = isConnected && !isSendingCommand,
                onColorSelected = onColorChange,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Effect selector
            EffectSelector(
                selectedEffect = line.effectId,
                enabled = isConnected && !isSendingCommand,
                onEffectSelected = onEffectChange,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Speed slider
            Text(
                text = "Speed: ${line.speed}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = line.speed.toFloat(),
                onValueChange = { onSpeedChange(it.toInt()) },
                valueRange = 0f..255f,
                steps = 50,
                enabled = isConnected && !isSendingCommand,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Line brightness
            Text(
                text = "Line Brightness: ${line.brightness.toPercent()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = line.brightness.value.toFloat(),
                onValueChange = { onBrightnessChange(Brightness(it.toInt())) },
                valueRange = 0f..255f,
                steps = 50,
                enabled = isConnected && !isSendingCommand,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AllLinesSection(
    lines: List<LedLineState>,
    isConnected: Boolean,
    isSendingCommand: Boolean,
    onColorChange: (RgbColor) -> Unit,
    onEffectChange: (EffectId) -> Unit,
    onSpeedChange: (Int) -> Unit,
    onBrightnessChange: (Brightness) -> Unit,
) {
    // Use the first line's state as a reference for "All"
    val referenceLine = lines.firstOrNull() ?: return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "All Lines",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Color",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            ColorSelector(
                selectedColor = referenceLine.color,
                enabled = isConnected && !isSendingCommand,
                onColorSelected = onColorChange,
            )

            Spacer(modifier = Modifier.height(16.dp))

            EffectSelector(
                selectedEffect = referenceLine.effectId,
                enabled = isConnected && !isSendingCommand,
                onEffectSelected = onEffectChange,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Speed: ${referenceLine.speed}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = referenceLine.speed.toFloat(),
                onValueChange = { onSpeedChange(it.toInt()) },
                valueRange = 0f..255f,
                steps = 50,
                enabled = isConnected && !isSendingCommand,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Brightness: ${referenceLine.brightness.toPercent()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = referenceLine.brightness.value.toFloat(),
                onValueChange = { onBrightnessChange(Brightness(it.toInt())) },
                valueRange = 0f..255f,
                steps = 50,
                enabled = isConnected && !isSendingCommand,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ColorSelector(
    selectedColor: RgbColor,
    enabled: Boolean,
    onColorSelected: (RgbColor) -> Unit,
) {
    val presetColors = listOf(
        RgbColor.White,
        RgbColor.Red,
        RgbColor(255, 165, 0), // Orange
        RgbColor(255, 255, 0),
        RgbColor.Green,
        RgbColor(0, 255, 255), // Cyan
        RgbColor.Blue,
        RgbColor(128, 0, 128), // Purple
        RgbColor(255, 192, 203), // Pink
        RgbColor(255, 105, 180), // Hot Pink
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(presetColors) { color ->
            val isSelected = color == selectedColor
            val composeColor = Color(color.red / 255f, color.green / 255f, color.blue / 255f)

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(composeColor)
                    .then(
                        if (isSelected) {
                            Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        } else Modifier
                    )
                    .then(
                        if (enabled) {
                            Modifier.clickable { onColorSelected(color) }
                        } else Modifier
                    ),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EffectSelector(
    selectedEffect: EffectId,
    enabled: Boolean,
    onEffectSelected: (EffectId) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Text(
        text = "Effect",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(6.dp))

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
    ) {
        OutlinedTextField(
            value = selectedEffect.displayName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            enabled = enabled,
            singleLine = true,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            EffectId.entries.forEach { effect ->
                DropdownMenuItem(
                    text = { Text(effect.displayName) },
                    onClick = {
                        onEffectSelected(effect)
                        expanded = false
                    },
                )
            }
        }
    }
}
