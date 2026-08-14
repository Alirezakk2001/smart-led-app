package com.technest.smartled.feature.effects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.technest.smartled.core.model.EffectId

@Composable
fun EffectsScreen(
    viewModel: EffectsViewModel,
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Effects",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Browse and apply LED effects",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Effect list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.effects, key = { it.effectId }) { effect ->
                EffectCard(
                    effect = effect,
                    isSelected = state.selectedEffectId == effect.effectId,
                    isConnected = state.isConnected,
                    onClick = { viewModel.selectEffect(effect.effectId) },
                )
            }
        }

        // Parameter editor and apply buttons
        if (state.selectedEffectId != null) {
            Spacer(modifier = Modifier.height(12.dp))
            EffectParameterEditor(
                parameters = state.editingParameters,
                isConnected = state.isConnected,
                isSendingCommand = state.isSendingCommand,
                onParameterChange = { index, value -> viewModel.updateParameter(index, value) },
                onApplyToLine = { lineIndex -> viewModel.applyToLine(lineIndex) },
                onApplyToAll = { viewModel.applyToAllLines() },
            )
        }

        // Feedback messages
        if (state.appliedMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = state.appliedMessage!!,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { viewModel.clearAppliedMessage() }) {
                        Text("Dismiss")
                    }
                }
            }
        }

        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
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

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun EffectCard(
    effect: EffectInfo,
    isSelected: Boolean,
    isConnected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderModifier = if (isSelected) {
        Modifier.border(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
        )
    } else Modifier

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier)
            .clickable(enabled = isConnected) { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EffectIndicator(effect.effectId)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = effect.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = effect.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun EffectIndicator(effectId: EffectId) {
    val color = when (effectId) {
        EffectId.Static -> Color(0xFFFFFFFF)
        EffectId.Rainbow -> Color(0xFFFF00FF)
        EffectId.Breathing -> Color(0xFF66BBFF)
        EffectId.Fire -> Color(0xFFFF6600)
        EffectId.Twinkle -> Color(0xFFFFFF66)
        EffectId.Meteor -> Color(0xFFFFAA44)
        EffectId.Wave -> Color(0xFF44AAFF)
        EffectId.Police -> Color(0xFFFF4444)
        EffectId.Gradient -> Color(0xFFAA44FF)
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
private fun EffectParameterEditor(
    parameters: List<EffectParameter>,
    isConnected: Boolean,
    isSendingCommand: Boolean,
    onParameterChange: (Int, Int) -> Unit,
    onApplyToLine: (Int) -> Unit,
    onApplyToAll: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Parameters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (parameters.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No adjustable parameters for this effect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                parameters.forEachIndexed { index, param ->
                    Spacer(modifier = Modifier.height(12.dp))
                    ParameterSlider(
                        name = param.name,
                        value = param.value,
                        min = param.min,
                        max = param.max,
                        enabled = isConnected && !isSendingCommand,
                        onValueChange = { onParameterChange(index, it) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onApplyToLine(0) },
                    enabled = isConnected && !isSendingCommand,
                    modifier = Modifier.weight(1f),
                ) {
                    if (isSendingCommand) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Apply to Line 1")
                    }
                }
                Button(
                    onClick = onApplyToAll,
                    enabled = isConnected && !isSendingCommand,
                    modifier = Modifier.weight(1f),
                ) {
                    if (isSendingCommand) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Apply to All")
                    }
                }
            }
        }
    }
}

@Composable
private fun ParameterSlider(
    name: String,
    value: Int,
    min: Int,
    max: Int,
    enabled: Boolean,
    onValueChange: (Int) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "$value",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            steps = (max - min) / 5,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
