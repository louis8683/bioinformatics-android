package com.louislu.pennbioinformatics.ble

import com.louislu.pennbioinformatics.ble.model.BioinfoEntry
import com.louislu.pennbioinformatics.ble.model.BleDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BleRepository {
    // Device discovery
    fun getScannedDevices(): Flow<List<BleDevice>>
    fun startScan()
    fun stopScan()
    fun setDeviceNameFilter(name: String?)
    fun isScanning(): StateFlow<Boolean>
    fun clearResults()

    // Device connection
    fun connectToBioinfoDevice(device: BleDevice)
    suspend fun disconnectDevice()
    val isConnected: StateFlow<Boolean>
    val isConnecting: StateFlow<Boolean>
    fun getBioinfoData(): Flow<BioinfoEntry?>

    // Bluetooth
    fun isEnabled(): Boolean
}