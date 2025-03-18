package com.louislu.pennbioinformatics.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.louislu.pennbioinformatics.ble.model.BioinfoEntry
import com.louislu.pennbioinformatics.ble.model.BleDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

class BleRepositoryImpl(private val context: Context) : BleRepository {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
    private val scanner: BluetoothLeScanner? = bluetoothAdapter.bluetoothLeScanner

    private val _scannedDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    override fun getScannedDevices(): Flow<List<BleDevice>> = _scannedDevices

    private var scanCallback: ScanCallback? = null
    private val scanHandler = Handler(Looper.getMainLooper())

    private var deviceNameFilter: String? = null

    private val _isScanning = MutableStateFlow(false) // Flow to track scanning state
    override fun isScanning(): StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override fun isConnected(): StateFlow<Boolean> = _isConnected

    private var bioinfoBleManager: BioinfoBleManager = BioinfoBleManager(context)

    override fun getBioinfoData(): Flow<BioinfoEntry?> {
        return bioinfoBleManager.getBioinfoData()
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun startScan() {
        // Check required permissions before scanning
        ensurePermission(Manifest.permission.BLUETOOTH_SCAN) { throw BleScanPermissionNotGrantedException() }
        ensurePermission(Manifest.permission.BLUETOOTH_CONNECT) { throw BleConnectPermissionNotGrantedException() }

        if (scanner == null) {
            Timber.e("BLE Scanner is NULL! Make sure Bluetooth is ON.")
            throw BleScannerNotAvailableException()
        }

        if (_isScanning.value) {
            Timber.w("Already scanning, ignoring request.")
            return
        }

        Timber.d("Starting BLE scan...")
        _isScanning.value = true

        scanCallback = object : ScanCallback() {
            @SuppressLint("MissingPermission")
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val newDevice = BleDevice(device.name, device.address)

                _scannedDevices.update { currentList ->
                    if (newDevice !in currentList) currentList + newDevice else currentList
                }
                Timber.i("Device Found: Name=${device.name ?: "Unknown"}, Address=${device.address}")
            }

            override fun onScanFailed(errorCode: Int) {
                Timber.e("BLE Scan Failed: Error Code $errorCode")
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val filters = if (deviceNameFilter.isNullOrEmpty()) {
            null // Scan for all devices if no filter is applied
        } else {
            listOf(
                ScanFilter.Builder()
                    .setDeviceName(deviceNameFilter) // Filter by name
                    .build()
            )
        }

        scanner.startScan(filters, settings, scanCallback!!)
        scanHandler.postDelayed({ stopScan() }, SCAN_TIME_MS)
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun stopScan() {
        // Ensure `BLUETOOTH_SCAN` permission before stopping scan
        ensurePermission(Manifest.permission.BLUETOOTH_SCAN) { throw BleScanPermissionNotGrantedException() }
        ensurePermission(Manifest.permission.BLUETOOTH_CONNECT) { throw BleConnectPermissionNotGrantedException() }

        if (_isScanning.value && scanCallback != null) {
            scanner?.stopScan(scanCallback!!)
            Timber.d("Stopped BLE scan")
            _isScanning.value = false
        }
    }

    override fun setDeviceNameFilter(name: String?) {
        Timber.d("Setting device name filter: $name")
        deviceNameFilter = name
    }

    override fun clearResults() {
        _scannedDevices.value = emptyList()
        Timber.d("Cleared scan results")
    }

    override fun connectToBioinfoDevice(device: BleDevice) {
        val bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.address)
        if (bluetoothDevice == null) {
            Timber.e("Cannot connect: Device not found")
            return
        }

        bioinfoBleManager.connectToDevice(bluetoothDevice)

        _isConnected.value = true
    }

    override fun disconnectDevice() {
        bioinfoBleManager?.disconnectDevice()
        _isConnected.value = false
    }

    // Extracted function to reduce duplicated permission checks
    private fun ensurePermission(permission: String, onPermissionDenied: () -> Unit) {
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            onPermissionDenied()
        }
    }

    companion object {
        private const val SCAN_TIME_MS = 10000L
    }
}