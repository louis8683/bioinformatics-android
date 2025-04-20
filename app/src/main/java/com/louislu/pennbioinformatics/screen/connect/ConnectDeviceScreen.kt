package com.louislu.pennbioinformatics.screen.connect

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.louislu.pennbioinformatics.ble.BleViewModel
import com.louislu.pennbioinformatics.ble.model.BleDevice
import kotlinx.coroutines.launch

@Composable
fun ConnectDeviceScreenRoot(
    bleViewModel: BleViewModel,
    onConnected: () -> Unit,
    onSkip: () -> Unit
) {
    val devices by bleViewModel.scannedDevice.collectAsState(initial = listOf())
    val isScanning by bleViewModel.isScanning.collectAsState()
    val isConnected by bleViewModel.isConnected.collectAsState()
    val isConnecting by bleViewModel.isConnecting.collectAsState()
    var wasConnecting by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isConnected) {
        if (isConnected) {
            onConnected()
        }
    }

    LaunchedEffect(isConnecting, isConnected) {
        if (wasConnecting && !isConnecting && !isConnected) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Connection failed. Please try again.")
            }
        }
        wasConnecting = isConnecting
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (!bleViewModel.isEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Bluetooth is disabled. Please enable it to connect to device.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onSkip) {
                        Text("Skip")
                    }
                }
            }
        }
        else {
            Box(Modifier.padding(padding)) {
                ConnectDeviceScreen(
                    devices = devices,
                    onScan = {
                        bleViewModel.clearResults()
                        bleViewModel.startScan()
                    },
                    onConnect = {
                        bleViewModel.stopScan()
                        bleViewModel.connect(it)
                    },
                    isScanning = isScanning,
                    isConnecting = isConnecting,
                    onSkip = onSkip,
                )
            }
        }
    }
}

@Composable
fun ConnectDeviceScreen(
    devices: List<BleDevice>,
    onScan: () -> Unit,
    onConnect: (BleDevice) -> Unit,
    isScanning: Boolean,
    isConnecting: Boolean,
    onSkip: () -> Unit
) {
    var selectedDevice by remember { mutableStateOf<BleDevice?>(
        if (devices.isNotEmpty()) devices[0] else null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Texts

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
//            Text("The Bioinformatics App", style = MaterialTheme.typography.titleLarge)
            Text("Let’s connect!", style = MaterialTheme.typography.titleLarge)
            Text("Please select the device to connect", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Device List in the Center
        if (isScanning) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
        }
        if (devices.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(devices) { device ->
                    val isSelected = selectedDevice == device

                    ListItem(
                        headlineContent = { device.name?.let { Text(it) } ?: Text("(no name)") },
//                        overlineContent = { Text(text = "(not implemented)") },
//                        supportingContent = {
//                            if (device.address.isNotEmpty()) Text("ID: ${device.address}")
//                            else Text(text = "(no ID)")
//                        },
                        colors = ListItemDefaults.colors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDevice = device }
                    )
                    HorizontalDivider()
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No devices found", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedButton(
                onClick = onScan,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isConnecting
            ) {
                Text("Scan for Devices")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    selectedDevice?.let { onConnect(it) }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = devices.isNotEmpty() && selectedDevice != null && !isConnecting
            ) {
                if (isConnecting) CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                ) else Text("Connect")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun ConnectDeviceScreenPreview() {
    val sampleDevices = listOf(
        BleDevice(name = "Custom Sensor A", address = "44:38:39:ff:ef:57"),
        BleDevice(name = "Custom Sensor B", address = "55:22:88:aa:bb:cc"),
        BleDevice(name = "Custom Sensor C", address = "66:99:77:11:33:22")
    )
    ConnectDeviceScreen(devices = sampleDevices, onScan = {}, onConnect = {}, false, false, {})
}

@Preview(
    showBackground = true,
    widthDp = 320,
    heightDp = 568,
    name = "Small Screen Preview"
)
@Composable
fun ConnectDeviceScreenSmallPreview() {
    val sampleDevices = listOf(
        BleDevice(name = "Custom Sensor A", address = "44:38:39:ff:ef:57"),
        BleDevice(name = "Custom Sensor B", address = "55:22:88:aa:bb:cc"),
        BleDevice(name = "Custom Sensor C", address = "66:99:77:11:33:22"),
        BleDevice(name = "Custom Sensor D", address = "55:22:88:aa:bb:cc"),
        BleDevice(name = "Custom Sensor E", address = "55:22:88:aa:bb:cc"),
        BleDevice(name = "Custom Sensor F", address = "55:22:88:aa:bb:cc"),
        BleDevice(name = "Custom Sensor G", address = "55:22:88:aa:bb:cc"),
    )
    ConnectDeviceScreen(devices = sampleDevices, onScan = {}, onConnect = {}, false, false, {})
}