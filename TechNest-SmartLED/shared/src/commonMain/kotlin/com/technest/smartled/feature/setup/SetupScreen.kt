package com.technest.smartled.feature.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    onSetupComplete: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.isComplete) {
            SetupCompleteContent(onContinue = onSetupComplete)
        } else {
            SetupFormContent(
                state = state,
                onLineCountChanged = { viewModel.setLineCount(it) },
                onLedsForLineChanged = { index, count -> viewModel.setLedsForLine(index, count) },
                onApply = { viewModel.applyConfiguration() },
                onClearError = { viewModel.clearError() },
            )
        }
    }
}

@Composable
private fun SetupCompleteContent(onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Configuration Applied",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Your LED hardware has been configured successfully.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continue to Dashboard")
        }
    }
}

@Composable
private fun SetupFormContent(
    state: SetupUiState,
    onLineCountChanged: (Int) -> Unit,
    onLedsForLineChanged: (Int, Int) -> Unit,
    onApply: () -> Unit,
    onClearError: () -> Unit,
) {
    Text(
        text = "Hardware Setup",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Configure your LED hardware before you start.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Device info
    if (state.device != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                InfoRow(label = "Device", value = state.device.name)
                InfoRow(label = "Address", value = state.device.address)
                InfoRow(
                    label = "Max Lines",
                    value = state.capabilities.maxLines.toString(),
                )
                InfoRow(
                    label = "Max LEDs/Line",
                    value = state.capabilities.maxLedsPerLine.toString(),
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }

    // Line count selector
    SectionHeader(title = "Number of LED Lines")
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Lines",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
                onClick = { onLineCountChanged(state.lineCount - 1) },
                enabled = state.lineCount > 1,
                modifier = Modifier.size(36.dp),
            ) {
                Text("-", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = "${state.lineCount}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            FilledIconButton(
                onClick = { onLineCountChanged(state.lineCount + 1) },
                enabled = state.lineCount < state.capabilities.maxLines,
                modifier = Modifier.size(36.dp),
            ) {
                Text("+", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // LEDs per line
    SectionHeader(title = "LEDs per Line")
    state.ledsPerLine.forEachIndexed { index, ledCount ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Line ${index + 1}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(
                    onClick = { onLedsForLineChanged(index, ledCount - 10) },
                    enabled = ledCount > 10,
                    modifier = Modifier.size(32.dp),
                ) {
                    Text("-", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = "$ledCount",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                FilledIconButton(
                    onClick = { onLedsForLineChanged(index, ledCount + 10) },
                    enabled = ledCount < state.capabilities.maxLedsPerLine,
                    modifier = Modifier.size(32.dp),
                ) {
                    Text("+", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    // Error message
    state.errorMessage?.let { message ->
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
                    text = message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClearError) {
                    Text("Dismiss")
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Apply button
    Button(
        onClick = onApply,
        enabled = !state.isConfiguring,
        modifier = Modifier.fillMaxWidth().height(48.dp),
    ) {
        if (state.isConfiguring) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Applying...")
        } else {
            Text("Apply Configuration", style = MaterialTheme.typography.titleMedium)
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
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
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
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
