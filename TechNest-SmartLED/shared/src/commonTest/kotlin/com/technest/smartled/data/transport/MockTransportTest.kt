package com.technest.smartled.data.transport

import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MockTransportTest {

    @Test
    fun `initial state is disconnected`() {
        val transport = MockTransport()
        assertFalse(transport.isConnected())
    }

    @Test
    fun `connect succeeds and updates state`() = kotlinx.coroutines.test.runTest {
        val transport = MockTransport()

        val result = transport.connect("00:11:22:33:44:01")

        assertTrue(result.isSuccess)
        assertTrue(transport.isConnected())
    }

    @Test
    fun `disconnect after connect clears state`() = kotlinx.coroutines.test.runTest {
        val transport = MockTransport()

        transport.connect("00:11:22:33:44:01")
        assertTrue(transport.isConnected())

        transport.disconnect()
        assertFalse(transport.isConnected())
    }

    @Test
    fun `send fails when not connected`() = kotlinx.coroutines.test.runTest {
        val transport = MockTransport()

        val result = transport.send(byteArrayOf(1, 2, 3))

        assertTrue(result.isFailure)
    }

    @Test
    fun `send succeeds when connected`() = kotlinx.coroutines.test.runTest {
        val transport = MockTransport()

        transport.connect("00:11:22:33:44:01")
        val result = transport.send(byteArrayOf(1, 2, 3))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `scan discovers devices`() = kotlinx.coroutines.test.runTest {
        val transport = MockTransport()

        val devices = mutableListOf<ScannedDevice>()
        transport.startScan().collect { device ->
            devices.add(device)
        }

        assertEquals(3, devices.size)
        assertEquals("00:11:22:33:44:01", devices[0].address)
        assertEquals("LED Controller 1", devices[0].name)
    }

    @Test
    fun `events emit Connected on connect`() = kotlinx.coroutines.test.runTest {
        val transport = MockTransport()
        val events = mutableListOf<TransportEvent>()

        // Collect events in background
        val job = backgroundScope.launch {
            transport.events.collect { events.add(it) }
        }

        transport.connect("00:11:22:33:44:01")

        kotlinx.coroutines.delay(100) // let event propagate
        job.cancel()

        assertTrue(events.any { it is TransportEvent.Connected })
    }
}
