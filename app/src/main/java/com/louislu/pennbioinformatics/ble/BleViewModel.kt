package com.louislu.pennbioinformatics.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louislu.pennbioinformatics.auth.AuthRepository
import com.louislu.pennbioinformatics.ble.model.BioinfoEntry
import com.louislu.pennbioinformatics.ble.model.BleDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BleViewModel @Inject constructor(
    private val bleRepository: BleRepository
): ViewModel() {

    val isScanning: StateFlow<Boolean> = bleRepository.isScanning()
    val scannedDevice = bleRepository.getScannedDevices()
        .map { devices ->
            devices.filter { device ->
                device.name?.contains("bioinfo") ?: false
            }
        }
    val isConnected: StateFlow<Boolean> = bleRepository.isConnected()
    val bioInfoData = bleRepository.getBioinfoData()

    fun startScan() { bleRepository.startScan() }
    fun stopScan() { bleRepository.stopScan() }
    fun clearResults() { bleRepository.clearResults() }

    fun connect(device: BleDevice) { bleRepository.connectToBioinfoDevice(device) }
    fun disconnect() { bleRepository.disconnectDevice() }

}