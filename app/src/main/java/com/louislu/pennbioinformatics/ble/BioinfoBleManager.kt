package com.louislu.pennbioinformatics.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import com.louislu.pennbioinformatics.ble.model.BioinfoEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.exception.DeviceDisconnectedException
import no.nordicsemi.android.ble.exception.InvalidRequestException
import no.nordicsemi.android.ble.exception.RequestFailedException
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.UUID

class BioinfoBleManager(context: Context): BleManager(context) {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting

    private var handshakeResponse: String? = null
    private var handshakeJob: Job? = null

    companion object {
        private val REQUEST_CHARACTERISTIC_UUID = UUID.fromString("4f2d7b8e-23b9-4bc7-905f-a8e3d7841f6a")
        private val RESPONSE_CHARACTERISTIC_UUID = UUID.fromString("93e89c7d-65e3-41e6-b59f-1f3a6478de45")
        private val ENV_SENSING_SERVICE_UUID = UUID.fromString("0000181a-0000-1000-8000-00805f9b34fb")
        private val BATTERY_SERVICE_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        private val BIOINFO_CHARACTERISTIC_UUID = UUID.fromString("9fda7cce-48d4-4b1a-9026-6d46eec4e63a")

        private const val HANDSHAKE_MSG = "hello"
        private const val HANDSHAKE_RESPONSE = "howdy"
        private const val HANDSHAKE_TIMEOUT = 5000L // 5 seconds
    }

    private var requestCharacteristic: android.bluetooth.BluetoothGattCharacteristic? = null
    private var responseCharacteristic: android.bluetooth.BluetoothGattCharacteristic? = null
    private var bioinfoCharacteristic: android.bluetooth.BluetoothGattCharacteristic? = null

    private val _bioinfoFlow = MutableStateFlow<BioinfoEntry?>(null)
    val bioinfoFlow = _bioinfoFlow.asStateFlow()

    override fun initialize() {
        Timber.d("Initializing BioinfoBleManager")

        // Subscribe to notifications from the response characteristic
        responseCharacteristic?.let {

            enableIndications(it).with { _, data ->
                Timber.d("Handling response with size (${data.size()})")
                handleHandshakeResponse(data)
            }.enqueue()
            Timber.d("Subscribed to response indication")
        }
    }

    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        val service = gatt.getService(ENV_SENSING_SERVICE_UUID) // Your Service UUID
        requestCharacteristic = service?.getCharacteristic(REQUEST_CHARACTERISTIC_UUID)
        responseCharacteristic = service?.getCharacteristic(RESPONSE_CHARACTERISTIC_UUID)
        bioinfoCharacteristic = service?.getCharacteristic(BIOINFO_CHARACTERISTIC_UUID)
        Timber.d("check result: $requestCharacteristic && $responseCharacteristic && $bioinfoCharacteristic")
        return requestCharacteristic != null && responseCharacteristic != null && bioinfoCharacteristic != null
    }

    override fun onServicesInvalidated() {
        Timber.d("Services invalidated")
        requestCharacteristic = null
        responseCharacteristic = null
        bioinfoCharacteristic = null

        _isConnected.value = false
        handshakeJob?.cancel()
    }

    fun connectToDevice(device: BluetoothDevice) {
        _isConnecting.value = true
        connect(device)
            .timeout(10000)
            .useAutoConnect(false)
            .done {
                Timber.d("Connected to ${device.address}")
//                isConnected = true
                performHandshake()
            }
            .fail { _, status ->
                Timber.e("Failed to connect: $status")
                _isConnecting.value = false
            }
            .enqueue()
    }

    fun getBioinfoData(): Flow<BioinfoEntry?> = flow {
        while (true) {
            try {
                // Ensure device is connected and characteristic is available
                if (!_isConnected.value || bioinfoCharacteristic == null) {
                    Timber.e("Cannot read bioinfo data: Device is not connected or characteristic is null")
                    delay(5000) // Wait before retrying
                    continue
                }

                // Step 1: Read characteristic safely
                lateinit var bioinfoData: Data
                readCharacteristic(bioinfoCharacteristic!!).await (
                    object : DataReceivedCallback {
                        override fun onDataReceived(device: BluetoothDevice, data: Data) {
                            bioinfoData = data
                        }
                    }
                )

                // Step 2: Parse received data
                val parsedData = parseBioinfoData(bioinfoData)

                // Step 3: Emit the parsed data
                _bioinfoFlow.value = parsedData
                emit(parsedData)

            } catch (e: Exception) {
                Timber.e("Failed to read bioinfo data: ${e.message}")
            }

            delay(5000) // Read every 5 seconds
        }
    }.flowOn(Dispatchers.IO)

    private fun parseBioinfoData(data: Data?): BioinfoEntry? {
        if (data == null || data.value == null || data.value!!.size != 20) {
            Timber.e("Invalid bioinfo data received!")
            return null
        }

        if (data.value != null) {
            val buffer = ByteBuffer.wrap(data.value!!).order(ByteOrder.LITTLE_ENDIAN)
            return BioinfoEntry(
                temperature = buffer.float,
                humidity = buffer.float,
                pm25 = buffer.float,
                coConcentration = buffer.float,
                lastUpdate = buffer.int
            ).also {
                Timber.d("Parsed Bioinfo Data: $it")
            }
        } else {
            Timber.d("Parsed Bioinfo Data: data is null")
            return null
        }
    }

    private fun performHandshake() {

        Timber.d("Performing Handshake")

        if (requestCharacteristic == null) {
            Timber.e("Handshake failed: Request characteristic is NULL")
            _isConnecting.value = false
            return
        }

        handshakeJob = CoroutineScope(Dispatchers.IO).launch {
            val handshakeMessage = HANDSHAKE_MSG.toByteArray(StandardCharsets.UTF_8)

            try {
                writeCharacteristic(
                    requestCharacteristic,
                    handshakeMessage,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
                    .done { Timber.d("Handshake message sent successfully") }
                    .fail { _, status -> Timber.e("Failed to write handshake message: $status") }
                    .await()

                Timber.d("Sent message")

                withTimeout(HANDSHAKE_TIMEOUT) {
                    waitForIndication(responseCharacteristic)
                        .timeout(HANDSHAKE_TIMEOUT)
                        .await(
                            object : DataReceivedCallback {
                                override fun onDataReceived(device: BluetoothDevice, data: Data) {
                                    handleHandshakeResponse(data)
                                }
                            }
                        )
                }

                if (handshakeResponse == HANDSHAKE_RESPONSE) {
                    Timber.d("Handshake successful")
                    _isConnected.value = true
                } else {
                    Timber.e("Handshake failed: Bad response ($handshakeResponse)")
                    disconnectDevice()
                }
            } catch (e: TimeoutCancellationException) {
                Timber.e("Handshake failed: TIMEOUT")
                disconnectDevice()
            } catch (e: InterruptedException) {
                Timber.e("Handshake failed: TIMEOUT")
                disconnectDevice()
            } catch (e: RequestFailedException) {
                Timber.e("Handshake failed: request failed")
                disconnectDevice()
            } catch (e: DeviceDisconnectedException) {
                Timber.e("Handshake failed: device disconnected")
                disconnectDevice()
            } finally {
                _isConnecting.value = false
            }
        }
    }

    private fun handleHandshakeResponse(data: Data?) {
        val response = data?.getStringValue(0)
        handshakeResponse = response
        Timber.d("Received response: $response")
    }

    suspend fun disconnectDevice() = withContext(Dispatchers.IO) {
        Timber.e("Connection status: ${_isConnected.value}")
        Timber.e("Disconnecting...")
        try {
            disconnect()
                .timeout(HANDSHAKE_TIMEOUT)
                .await()
            _isConnected.value = false
        } catch (e: InvalidRequestException) {
            Timber.e("Already disconnected")
        }
        Timber.e("Disconnected")
    }
}