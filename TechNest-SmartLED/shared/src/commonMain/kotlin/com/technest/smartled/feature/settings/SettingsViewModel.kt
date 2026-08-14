package com.technest.smartled.feature.settings

import com.technest.smartled.core.model.DeviceConfiguration
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

enum class ThemeMode { System, Dark, Light }

data class SettingsUiState(
    val isConnected: Boolean = false,
    val deviceName: String = "",
    val deviceAddress: String = "",
    val firmwareVersion: String = "\u2014",
    val protocolVersion: Int = 0,
    val hardwareVersion: String = "\u2014",
    val lineCount: Int = 1,
    val ledsPerLine: List<Int> = listOf(60),
    val isApplyingConfig: Boolean = false,
    val configAppliedMessage: String? = null,
    val errorMessage: String? = null,
    val themeMode: ThemeMode = ThemeMode.System,
)

class SettingsViewModel(
    private val repository: DeviceRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            repository.observeConnectedDevice().collect { device ->
                _uiState.update { state ->
                    if (device != null) {
                        state.copy(
                            isConnected = true,
                            deviceName = device.name,
                            deviceAddress = device.address,
                            firmwareVersion = device.capabilities.firmwareVersion,
                            protocolVersion = device.capabilities.protocolVersion,
                            hardwareVersion = device.capabilities.hardwareVersion,
                            lineCount = device.configuration.lineCount,
                            ledsPerLine = device.configuration.ledsPerLine,
                        )
                    } else {
                        state.copy(
                            isConnected = false,
                            deviceName = "",
                            deviceAddress = "",
                            firmwareVersion = "\u2014",
                            protocolVersion = 0,
                            hardwareVersion = "\u2014",
                        )
                    }
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setLineCount(count: Int) {
        val clamped = count.coerceIn(1, 8)
        _uiState.update { state ->
            val currentLeds = state.ledsPerLine
            val newLeds = if (clamped > currentLeds.size) {
                currentLeds + List(clamped - currentLeds.size) { 60 }
            } else if (clamped < currentLeds.size) {
                currentLeds.take(clamped)
            } else {
                currentLeds
            }
            state.copy(lineCount = clamped, ledsPerLine = newLeds)
        }
    }

    fun setLedsForLine(lineIndex: Int, count: Int) {
        val clamped = count.coerceIn(1, 512)
        _uiState.update { state ->
            val newLeds = state.ledsPerLine.toMutableList().also {
                if (lineIndex < it.size) it[lineIndex] = clamped
            }
            state.copy(ledsPerLine = newLeds)
        }
    }

    fun applyConfiguration() {
        val state = _uiState.value
        if (!state.isConnected) {
            _uiState.update { it.copy(errorMessage = "Not connected to a device") }
            return
        }

        val config = try {
            DeviceConfiguration(
                lineCount = state.lineCount,
                ledsPerLine = state.ledsPerLine,
            )
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message ?: "Invalid configuration") }
            return
        }

        _uiState.update {
            it.copy(isApplyingConfig = true, errorMessage = null, configAppliedMessage = null)
        }

        scope.launch {
            val result = repository.applyConfiguration(config)
            if (result is CommandResult.Failure) {
                _uiState.update {
                    it.copy(errorMessage = result.reason, isApplyingConfig = false)
                }
            } else {
                _uiState.update {
                    it.copy(
                        isApplyingConfig = false,
                        configAppliedMessage = "Configuration applied successfully",
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearConfigAppliedMessage() {
        _uiState.update { it.copy(configAppliedMessage = null) }
    }
}
