package com.technest.smartled.feature.effects

import com.technest.smartled.core.model.EffectId
import com.technest.smartled.core.model.EffectParameter
import com.technest.smartled.core.model.RgbColor
import com.technest.smartled.data.repository.CommandResult
import com.technest.smartled.data.repository.DeviceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EffectInfo(
    val effectId: EffectId,
    val name: String,
    val description: String,
    val defaultParameters: List<EffectParameter>,
)

data class EffectsUiState(
    val effects: List<EffectInfo> = emptyList(),
    val selectedEffectId: EffectId? = null,
    val editingParameters: List<EffectParameter> = emptyList(),
    val lineIndices: List<Int> = emptyList(),
    val isConnected: Boolean = false,
    val isSendingCommand: Boolean = false,
    val errorMessage: String? = null,
    val appliedMessage: String? = null,
)

fun getDefaultParameters(effectId: EffectId): List<EffectParameter> = when (effectId) {
    EffectId.Static -> emptyList()
    EffectId.Rainbow -> listOf(EffectParameter("Speed", 128, 0, 255))
    EffectId.Breathing -> listOf(EffectParameter("Speed", 64, 0, 255))
    EffectId.Fire -> listOf(EffectParameter("Speed", 100, 0, 255))
    EffectId.Twinkle -> listOf(EffectParameter("Speed", 128, 0, 255))
    EffectId.Meteor -> listOf(EffectParameter("Speed", 150, 0, 255))
    EffectId.Wave -> listOf(EffectParameter("Speed", 128, 0, 255))
    EffectId.Police -> listOf(EffectParameter("Speed", 100, 0, 255))
    EffectId.Gradient -> listOf(EffectParameter("Speed", 128, 0, 255))
}

fun getEffectDescription(effectId: EffectId): String = when (effectId) {
    EffectId.Static -> "Solid color display"
    EffectId.Rainbow -> "Smooth color cycling across the spectrum"
    EffectId.Breathing -> "Gentle fade in and out"
    EffectId.Fire -> "Dynamic flame simulation"
    EffectId.Twinkle -> "Random twinkling sparkles"
    EffectId.Meteor -> "Shooting meteor effect with trail"
    EffectId.Wave -> "Flowing wave across LEDs"
    EffectId.Police -> "Alternating red and blue flashing"
    EffectId.Gradient -> "Smooth color gradient transitions"
}

class EffectsViewModel(
    private val repository: DeviceRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _uiState = MutableStateFlow(EffectsUiState())
    val uiState: StateFlow<EffectsUiState> = _uiState.asStateFlow()

    private val effects: List<EffectInfo> = EffectId.entries.map { id ->
        EffectInfo(
            effectId = id,
            name = id.displayName,
            description = getEffectDescription(id),
            defaultParameters = getDefaultParameters(id),
        )
    }

    init {
        _uiState.update { it.copy(effects = effects) }

        scope.launch {
            repository.observeConnectedDevice().collect { device ->
                _uiState.update {
                    it.copy(
                        isConnected = device != null,
                        lineIndices = device?.state?.lines?.map { line -> line.index }.orEmpty(),
                    )
                }
            }
        }
    }

    fun selectEffect(effectId: EffectId) {
        val effect = effects.find { it.effectId == effectId } ?: return
        _uiState.update {
            it.copy(
                selectedEffectId = effectId,
                editingParameters = effect.defaultParameters.map { p -> p.copy() },
                appliedMessage = null,
            )
        }
    }

    fun updateParameter(index: Int, newValue: Int) {
        _uiState.update { state ->
            state.copy(
                editingParameters = state.editingParameters.mapIndexed { i, param ->
                    if (i == index) param.copy(value = newValue.coerceIn(param.min, param.max))
                    else param
                }
            )
        }
    }

    fun applyToLine(lineIndex: Int) {
        val state = _uiState.value
        val effectId = state.selectedEffectId ?: return
        if (!state.isConnected) {
            _uiState.update { it.copy(errorMessage = "Not connected to a device") }
            return
        }

        _uiState.update { it.copy(isSendingCommand = true, errorMessage = null, appliedMessage = null) }

        scope.launch {
            try {
                // Set the effect on the line
                val result = repository.setLineEffect(lineIndex, effectId)
                if (result is CommandResult.Failure) {
                    _uiState.update { it.copy(errorMessage = result.reason, isSendingCommand = false) }
                    return@launch
                }

                // Apply speed from parameters
                val speedParam = state.editingParameters.firstOrNull { it.name == "Speed" }
                if (speedParam != null) {
                    repository.setLineSpeed(lineIndex, speedParam.value)
                }

                _uiState.update {
                    it.copy(
                        isSendingCommand = false,
                        appliedMessage = "Applied ${effectId.displayName} to Line ${lineIndex + 1}",
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to apply effect", isSendingCommand = false)
                }
            }
        }
    }

    fun applyToAllLines() {
        val state = _uiState.value
        val effectId = state.selectedEffectId ?: return
        if (!state.isConnected) {
            _uiState.update { it.copy(errorMessage = "Not connected to a device") }
            return
        }

        _uiState.update { it.copy(isSendingCommand = true, errorMessage = null, appliedMessage = null) }

        scope.launch {
            try {
                val lines = repository.observeConnectedDevice().value?.state?.lines ?: emptyList()
                val speedParam = state.editingParameters.firstOrNull { it.name == "Speed" }

                for (line in lines) {
                    repository.setLineEffect(line.index, effectId)
                    if (speedParam != null) {
                        repository.setLineSpeed(line.index, speedParam.value)
                    }
                }

                _uiState.update {
                    it.copy(
                        isSendingCommand = false,
                        appliedMessage = "Applied ${effectId.displayName} to all lines",
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to apply effect", isSendingCommand = false)
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearAppliedMessage() {
        _uiState.update { it.copy(appliedMessage = null) }
    }
}
