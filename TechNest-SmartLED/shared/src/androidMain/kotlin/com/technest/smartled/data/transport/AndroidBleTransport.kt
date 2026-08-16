package com.technest.smartled.data.transport

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

private const val ScanDurationMs = 10_000L
private const val ConnectTimeoutMs = 12_000L

class AndroidBleTransport(
    private val context: Context,
) : Transport, TransportScanner {

    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter?
        get() = bluetoothManager.adapter

    private val _events = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 8)
    override val events: Flow<TransportEvent> = _events.asSharedFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private var connectedAddress: String? = null

    @SuppressLint("MissingPermission")
    override suspend fun startScan(): Flow<ScannedDevice> {
        val scanner = adapter?.bluetoothLeScanner
        if (adapter?.isEnabled != true || scanner == null) {
            return kotlinx.coroutines.flow.flow {
                throw IllegalStateException("Bluetooth is disabled or unavailable")
            }
        }
        if (!hasScanPermission()) {
            return kotlinx.coroutines.flow.flow {
                throw SecurityException(scanPermissionMessage())
            }
        }

        return callbackFlow {
            val seenAddresses = mutableSetOf<String>()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device ?: return
                val address = device.address ?: return
                if (!seenAddresses.add(address)) return

                val name = result.scanRecord?.deviceName
                    ?: safeDeviceName(device)
                    ?: "Unknown BLE Device"

                trySend(
                    ScannedDevice(
                        address = address,
                        name = name,
                        rssi = result.rssi,
                    )
                )
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
            }

            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException("BLE scan failed: $errorCode"))
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, callback)
        val timeoutJob: Job = launch {
            delay(ScanDurationMs)
            scanner.stopScan(callback)
            close()
        }

            awaitClose {
                timeoutJob.cancel()
                scanner.stopScan(callback)
            }
        }
    }

    override suspend fun stopScan() = Unit

    @SuppressLint("MissingPermission")
    override suspend fun connect(address: String): Result<Unit> {
        val bluetoothAdapter = adapter
            ?: return Result.failure(IllegalStateException("Bluetooth is unavailable"))
        if (!bluetoothAdapter.isEnabled) {
            return Result.failure(IllegalStateException("Bluetooth is disabled"))
        }
        if (!hasConnectPermission()) {
            return Result.failure(SecurityException(connectPermissionMessage()))
        }

        return try {
            disconnect()
            val device = bluetoothAdapter.getRemoteDevice(address)
            withTimeout(ConnectTimeoutMs) {
                suspendCancellableCoroutine { continuation ->
                    var gatt: BluetoothGatt? = null
                    val callback = object : BluetoothGattCallback() {
                        override fun onConnectionStateChange(gattInstance: BluetoothGatt, status: Int, newState: Int) {
                            when {
                                status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED -> {
                                    bluetoothGatt = gattInstance
                                    connectedAddress = address
                                    _events.tryEmit(TransportEvent.Connected)
                                    if (continuation.isActive) continuation.resume(Result.success(Unit))
                                }
                                newState == BluetoothProfile.STATE_DISCONNECTED -> {
                                    clearConnection(gattInstance)
                                    _events.tryEmit(TransportEvent.Disconnected)
                                    if (continuation.isActive) {
                                        continuation.resume(Result.failure(IllegalStateException("BLE connection disconnected")))
                                    }
                                }
                                status != BluetoothGatt.GATT_SUCCESS -> {
                                    clearConnection(gattInstance)
                                    val reason = "BLE connection failed: $status"
                                    _events.tryEmit(TransportEvent.Error(reason))
                                    if (continuation.isActive) {
                                        continuation.resume(Result.failure(IllegalStateException(reason)))
                                    }
                                }
                            }
                        }
                    }

                    gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        device.connectGatt(appContext, false, callback, BluetoothDevice.TRANSPORT_LE)
                    } else {
                        device.connectGatt(appContext, false, callback)
                    }

                    continuation.invokeOnCancellation {
                        gatt?.disconnect()
                        gatt?.close()
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() {
        val gatt = bluetoothGatt
        bluetoothGatt = null
        connectedAddress = null
        if (hasConnectPermission()) {
            gatt?.disconnect()
            gatt?.close()
        } else {
            gatt?.close()
        }
        _events.tryEmit(TransportEvent.Disconnected)
    }

    override suspend fun send(data: ByteArray): Result<Unit> {
        if (!isConnected()) {
            return Result.failure(IllegalStateException("Not connected"))
        }
        return Result.failure(UnsupportedOperationException("BLE protocol is not implemented yet"))
    }

    override fun isConnected(): Boolean = connectedAddress != null

    @SuppressLint("MissingPermission")
    private fun safeDeviceName(device: BluetoothDevice): String? {
        return if (hasConnectPermission()) device.name else null
    }

    @SuppressLint("MissingPermission")
    private fun clearConnection(gatt: BluetoothGatt) {
        if (bluetoothGatt === gatt) {
            bluetoothGatt = null
            connectedAddress = null
        }
        gatt.close()
    }

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            appContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun scanPermissionMessage(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            "Bluetooth scan permission is required"
        } else {
            "Location permission is required for BLE scan"
        }
    }

    private fun connectPermissionMessage(): String = "Bluetooth connect permission is required"
}
