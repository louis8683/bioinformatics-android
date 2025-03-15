package com.louislu.pennbioinformatics.ble

interface BleClient {
    suspend fun connect(deviceAddress: String): Boolean
    suspend fun sendHandshake(): String
}