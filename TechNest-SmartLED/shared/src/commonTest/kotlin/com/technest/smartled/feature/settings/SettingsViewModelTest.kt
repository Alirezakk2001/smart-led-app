package com.technest.smartled.feature.settings

import com.technest.smartled.core.model.DeviceConfiguration
import com.technest.smartled.data.repository.DeviceRepositoryImpl
import com.technest.smartled.data.transport.MockTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @Test
    fun `initial state has defaults`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertFalse(state.isConnected)
        assertEquals("", state.deviceName)
        assertEquals(1, state.lineCount)
        assertEquals(listOf(60), state.ledsPerLine)
        assertEquals(ThemeMode.System, state.themeMode)
    }

    @Test
    fun `setThemeMode updates theme`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.setThemeMode(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, viewModel.uiState.value.themeMode)

        viewModel.setThemeMode(ThemeMode.Light)
        assertEquals(ThemeMode.Light, viewModel.uiState.value.themeMode)

        viewModel.setThemeMode(ThemeMode.System)
        assertEquals(ThemeMode.System, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `setLineCount adjusts ledsPerLine list size`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.setLineCount(3)
        val state = viewModel.uiState.value
        assertEquals(3, state.lineCount)
        assertEquals(3, state.ledsPerLine.size)
        assertEquals(60, state.ledsPerLine[0])
        assertEquals(60, state.ledsPerLine[1])
        assertEquals(60, state.ledsPerLine[2])
    }

    @Test
    fun `setLineCount clamps to minimum 1`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.setLineCount(0)
        assertEquals(1, viewModel.uiState.value.lineCount)
    }

    @Test
    fun `setLineCount clamps to maximum 8`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.setLineCount(10)
        assertEquals(8, viewModel.uiState.value.lineCount)
    }

    @Test
    fun `setLineCount reduces ledsPerLine when decreasing`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()
        viewModel.setLineCount(3)
        assertEquals(3, viewModel.uiState.value.ledsPerLine.size)

        viewModel.setLineCount(1)
        val state = viewModel.uiState.value
        assertEquals(1, state.lineCount)
        assertEquals(1, state.ledsPerLine.size)
    }

    @Test
    fun `setLedsForLine updates specific line`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()
        viewModel.setLineCount(3)

        viewModel.setLedsForLine(0, 100)
        assertEquals(100, viewModel.uiState.value.ledsPerLine[0])
        assertEquals(60, viewModel.uiState.value.ledsPerLine[1])
        assertEquals(60, viewModel.uiState.value.ledsPerLine[2])
    }

    @Test
    fun `setLedsForLine clamps to valid range`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.setLedsForLine(0, 0)
        assertEquals(1, viewModel.uiState.value.ledsPerLine[0])

        viewModel.setLedsForLine(0, 999)
        assertEquals(512, viewModel.uiState.value.ledsPerLine[0])
    }

    @Test
    fun `isConnected becomes true when device connects`() = runTest(StandardTestDispatcher()) {
        val transport = MockTransport()
        val repository = DeviceRepositoryImpl(transport, transport)
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val viewModel = SettingsViewModel(repository, scope = scope)

        assertFalse(viewModel.uiState.value.isConnected)

        repository.scan()
        advanceUntilIdle()
        repository.connect("00:11:22:33:44:01")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isConnected)
        assertTrue(viewModel.uiState.value.deviceName.isNotEmpty())
    }

    @Test
    fun `applyConfiguration without connection shows error`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.applyConfiguration()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `applyConfiguration with connection succeeds`() = runTest(StandardTestDispatcher()) {
        val transport = MockTransport()
        val repository = DeviceRepositoryImpl(transport, transport)
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val viewModel = SettingsViewModel(repository, scope = scope)

        repository.scan()
        advanceUntilIdle()
        repository.connect("00:11:22:33:44:01")
        advanceUntilIdle()

        viewModel.setLineCount(2)
        viewModel.setLedsForLine(1, 120)

        viewModel.applyConfiguration()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertNotNull(viewModel.uiState.value.configAppliedMessage)
    }

    @Test
    fun `applyConfiguration updates device configuration`() = runTest(StandardTestDispatcher()) {
        val transport = MockTransport()
        val repository = DeviceRepositoryImpl(transport, transport)
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val viewModel = SettingsViewModel(repository, scope = scope)

        repository.scan()
        advanceUntilIdle()
        repository.connect("00:11:22:33:44:01")
        advanceUntilIdle()

        viewModel.setLineCount(2)
        viewModel.setLedsForLine(0, 50)
        viewModel.setLedsForLine(1, 100)

        viewModel.applyConfiguration()
        advanceUntilIdle()

        val device = repository.observeConnectedDevice().value
        assertNotNull(device)
        assertEquals(2, device.configuration.lineCount)
        assertEquals(listOf(50, 100), device.configuration.ledsPerLine)
    }

    @Test
    fun `clearError resets error message`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.applyConfiguration()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `clearConfigAppliedMessage resets applied message`() = runTest(StandardTestDispatcher()) {
        val transport = MockTransport()
        val repository = DeviceRepositoryImpl(transport, transport)
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val viewModel = SettingsViewModel(repository, scope = scope)

        repository.scan()
        advanceUntilIdle()
        repository.connect("00:11:22:33:44:01")
        advanceUntilIdle()

        viewModel.applyConfiguration()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.configAppliedMessage)

        viewModel.clearConfigAppliedMessage()
        assertNull(viewModel.uiState.value.configAppliedMessage)
    }

    @Test
    fun `connected device populates device info`() = runTest(StandardTestDispatcher()) {
        val transport = MockTransport()
        val repository = DeviceRepositoryImpl(transport, transport)
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val viewModel = SettingsViewModel(repository, scope = scope)

        repository.scan()
        advanceUntilIdle()
        repository.connect("00:11:22:33:44:01")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isConnected)
        assertEquals(1, state.lineCount)
        assertEquals(listOf(60), state.ledsPerLine)
    }

    private fun TestScope.createViewModel(): SettingsViewModel {
        val transport = MockTransport()
        val repository = DeviceRepositoryImpl(transport, transport)
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        return SettingsViewModel(repository, scope = scope)
    }
}
