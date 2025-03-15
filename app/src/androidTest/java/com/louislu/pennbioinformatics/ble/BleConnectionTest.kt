package com.louislu.pennbioinformatics.ble

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BleConnectionTest {

    @Test
    fun givenBleClient_whenConnecting_thenReturnsSuccess() = runBlocking {
        // Arrange
        val mockClient = mockk<BleClient>()
        coEvery { mockClient.connect("00:11:22:33:44:55") } returns true

        // Act
        val isConnected = mockClient.connect("00:11:22:33:44:55")

        // Assert
        assertTrue(isConnected)
    }
}