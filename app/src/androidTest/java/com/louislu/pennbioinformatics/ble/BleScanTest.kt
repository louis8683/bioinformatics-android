package com.louislu.pennbioinformatics.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import androidx.test.espresso.internal.inject.InstrumentationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

@RunWith(AndroidJUnit4::class)
class BleScanTest {

//    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private val bluetoothManager = InstrumentationRegistry
            .getInstrumentation()
            .context
            .getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter


    // Automatically grant Bluetooth permissions before running tests
    @get:Rule
    var mRuntimePermissionRule: GrantPermissionRule? = GrantPermissionRule
        .grant(Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION)

    @Test
    fun givenBluetoothAdapter_whenScanning_thenFindsDevices() = runBlocking {

        Timber.d("Starting BLE Scan Test")

        val scanner = bluetoothAdapter.bluetoothLeScanner
        assertNotNull(scanner)

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Max performance mode
            .build()

        var deviceFound = false
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                deviceFound = true
                Timber.i("Device Found: Name=${result.device.name}, Address=${result.device.address}")
            }

            override fun onScanFailed(errorCode: Int) {
                Timber.e("BLE Scan Failed with Error Code: $errorCode")
            }
        }

        Timber.d("Starting BLE scan...")
        scanner.startScan(null, scanSettings, scanCallback) // NULL means scan all devices
        Thread.sleep(5000) // Allow more time for scanning
        scanner.stopScan(scanCallback)
        Timber.d("Stopped BLE scan")

        assertTrue("No BLE devices found!", deviceFound)
    }


    @Test
    fun givenBluetoothAdapter_whenScanning_thenFindsDevices_comprehensive() = runBlocking {
        Timber.d("Starting BLE Scan Test")

        if (bluetoothAdapter == null) {
            Timber.e("Bluetooth Adapter is NULL!")
            return@runBlocking
        }

        if (!bluetoothAdapter.isEnabled) {
            Timber.e("Bluetooth is NOT enabled! Enable Bluetooth and retry.")
            return@runBlocking
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            Timber.e("Location services are OFF! Turn them ON and retry.")
            return@runBlocking
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner
        assertNotNull(scanner)

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

//        val filters = listOf(
//            ScanFilter.Builder().setDeviceName("bioinfo").build()
//        )

        var deviceFound = false
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                deviceFound = true
                Timber.i("Device Found: Name=${result.device.name}, Address=${result.device.address}")
            }

            override fun onScanFailed(errorCode: Int) {
                when (errorCode) {
                    ScanCallback.SCAN_FAILED_ALREADY_STARTED -> Timber.e("Scan failed: Already started")
                    ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> Timber.e("Scan failed: App registration failed")
                    ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> Timber.e("Scan failed: Internal error")
                    ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> Timber.e("Scan failed: Feature unsupported")
                    else -> Timber.e("Scan failed: Unknown error ($errorCode)")
                }
            }
        }

        Timber.d("Starting BLE scan...")
//        scanner.startScan(filters, scanSettings, scanCallback)
        scanner.startScan(null, scanSettings, scanCallback)
        Thread.sleep(10000) // Scan for 10 seconds
        scanner.stopScan(scanCallback)
        Timber.d("Stopped BLE scan")

        assertTrue("No BLE devices found!", deviceFound)
    }
}