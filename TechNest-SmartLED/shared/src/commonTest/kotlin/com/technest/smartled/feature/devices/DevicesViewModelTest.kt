package com.technest.smartled.feature.devices

import com.technest.smartled.core.model.ConnectionState
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
class DevicesViewModelTest {

    @Test
    fun `initial state is empty and not scanning`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue(state.discoveredDevices.isEmpty())
        assertNull(state.connectedDevice)
        assertFalse(state.isScanning)
        assertFalse(state.isConnecting)
        assertNull(state.errorMessage)
    }

    @Test
    fun `startScan populates device list`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.startScan()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.discoveredDevices.size)
        assertEquals("LED Controller 1", state.discoveredDevices[0].name)
        assertFalse(state.isScanning)
    }

    @Test
    fun `scan sets isScanning state`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.startScan()
        assertTrue(viewModel.uiState.value.isScanning)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isScanning)
    }

    @Test
    fun `connect updates connectedDevice`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.startScan()
        advanceUntilIdle()

        val deviceId = viewModel.uiState.value.discoveredDevices.first().id
        viewModel.connect(deviceId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.connectedDevice)
        assertEquals(deviceId, state.connectedDevice.id)
        assertEquals(ConnectionState.Connected, state.connectedDevice.connectionState)
        assertFalse(state.isConnecting)
    }

    @Test
    fun `connect to unknown device shows error`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.startScan()
        advanceUntilIdle()

        viewModel.connect("unknown-device")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.connectedDevice)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun `disconnect clears connectedDevice`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.startScan()
        advanceUntilIdle()

        val deviceId = viewModel.uiState.value.discoveredDevices.first().id
        viewModel.connect(deviceId)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.connectedDevice)

        viewModel.disconnect()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.connectedDevice)
    }

    @Test
    fun `clearError resets error message`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.startScan()
        advanceUntilIdle()
        viewModel.connect("unknown-device")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    private fun TestScope.createViewModel(): DevicesViewModel {
        val transport = MockTransport()
        val repository = DeviceRepositoryImpl(transport, transport)
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        return DevicesViewModel(repository, scope = scope)
    }
}
