package com.technest.smartled.data.repository

import com.technest.smartled.core.model.ConnectionState
import com.technest.smartled.data.transport.MockTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceRepositoryImplTest {

    @Test
    fun `scan discovers mock devices`() = kotlinx.coroutines.test.runTest {
        val transport = MockTransport()
        val repo = DeviceRepositoryImpl(transport, transport)

        val devices = repo.scan()

        assertEquals(3, devices.size)
        assertEquals("LED Controller 1", devices[0].name)
        assertEquals("00:11:22:33:44:01", devices[0].address)
    }

    @Test
    fun `connect returns success for known device`() = kotlinx.coroutines.test.runTest {
        val transport = MockTransport()
        val repo = DeviceRepositoryImpl(transport, transport)

        repo.scan()
        val result = repo.connect("00:11:22:33:44:01")

        assertTrue(result is ConnectionResult.Success)
        val device = result.device
        assertEquals("LED Controller 1", device.name)
        assertEquals(ConnectionState.Connected, device.connectionState)
    }

    @Test
    fun `connect returns failure for unknown device`() = kotlinx.coroutines.test.runTest {
        val transport = MockTransport()
        val repo = DeviceRepositoryImpl(transport, transport)

        repo.scan()
        val result = repo.connect("unknown-device")

        assertTrue(result is ConnectionResult.Failure)
    }

    @Test
    fun `connected device is observable via StateFlow`() = kotlinx.coroutines.test.runTest {
        val transport = MockTransport()
        val repo = DeviceRepositoryImpl(transport, transport)

        repo.scan()
        assertNull(repo.observeConnectedDevice().value)

        repo.connect("00:11:22:33:44:01")
        assertNotNull(repo.observeConnectedDevice().value)
        assertEquals("LED Controller 1", repo.observeConnectedDevice().value?.name)
    }

    @Test
    fun `disconnect clears connected device`() = kotlinx.coroutines.test.runTest {
        val transport = MockTransport()
        val repo = DeviceRepositoryImpl(transport, transport)

        repo.scan()
        repo.connect("00:11:22:33:44:01")
        assertNotNull(repo.observeConnectedDevice().value)

        repo.disconnect()
        assertNull(repo.observeConnectedDevice().value)
    }

    @Test
    fun `readCapabilities returns null when not connected`() = kotlinx.coroutines.test.runTest {
        val transport = MockTransport()
        val repo = DeviceRepositoryImpl(transport, transport)

        assertNull(repo.readCapabilities())
    }

    @Test
    fun `readCapabilities returns capabilities when connected`() = kotlinx.coroutines.test.runTest {
        val transport = MockTransport()
        val repo = DeviceRepositoryImpl(transport, transport)

        repo.scan()
        repo.connect("00:11:22:33:44:01")

        val caps = repo.readCapabilities()
        assertNotNull(caps)
        assertEquals(4, caps.maxLines)
        assertEquals(512, caps.maxLedsPerLine)
    }

    @Test
    fun `commands return failure when not connected`() = kotlinx.coroutines.test.runTest {
        val transport = MockTransport()
        val repo = DeviceRepositoryImpl(transport, transport)

        assertTrue(repo.setPower(true) is CommandResult.Failure)
        assertTrue(repo.setLineEnabled(0, true) is CommandResult.Failure)
        assertTrue(repo.applyConfiguration(
            com.technest.smartled.core.model.DeviceConfiguration()
        ) is CommandResult.Failure)
    }
}
