package com.louislu.pennbioinformatics.ble

interface BleScanner {
    suspend fun scan(): List<String>
}