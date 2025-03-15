package com.louislu.pennbioinformatics.ble

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BleHandshakeTest {

    @Test
    fun givenBleClient_whenSendingHandshake_thenReceivesExpectedResponse() = runBlocking {
        // Arrange
        val mockClient = mockk<BleClient>()
        coEvery { mockClient.sendHandshake() } returns "howdy"

        // Act
        val response = mockClient.sendHandshake()

        // Assert
        assertEquals("howdy", response)
    }
}