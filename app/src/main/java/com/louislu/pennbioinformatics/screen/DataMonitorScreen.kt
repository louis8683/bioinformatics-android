package com.louislu.pennbioinformatics.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.louislu.pennbioinformatics.ble.BleViewModel
import com.louislu.pennbioinformatics.domain.model.DataEntry
import com.louislu.pennbioinformatics.domain.model.Device
import com.louislu.pennbioinformatics.domain.model.generateFakeDataEntries
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Instant
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DataMonitorScreenRoot(
    bleViewModel: BleViewModel
) {
    val dataMonitorViewModel: DataMonitorViewModel = viewModel()

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var location by remember { mutableStateOf<Location?>(null) }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location = it }
        } else {
            // TODO: handle the case when permission is not granted (should be a rare case)
        }
    }

    val startTime by remember { mutableLongStateOf(Instant.now().toEpochMilli()) }
    val currentBioinfoEntry by bleViewModel.bioInfoData.collectAsState(initial = null)


    DataMonitorScreen(
        data = null,
        startTimeEpoch = startTime,
        location = null) {
    }
}

@Composable
fun DataMonitorScreen(
    data: DataEntry?,
    startTimeEpoch: Long,
    location: Location?,
    onEndSessionConfirmed: () -> Unit
) {
    var secondsElapsed by remember { mutableLongStateOf(0) }
    var text by remember { mutableStateOf("") }
    val openAlertDialog = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (isActive) {
            val now = System.currentTimeMillis()
            secondsElapsed = (now - startTimeEpoch) / 1000
            delay(1000L)
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Texts

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("The Bioinformatics App", style = MaterialTheme.typography.titleLarge)
                Text("Session ongoing...", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Session Duration: ${formatElapsedTime(secondsElapsed)}", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Location: ${formatLocation(location?.latitude ?: Double.MIN_VALUE, location?.longitude ?: Double.MIN_VALUE)}", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(modifier = Modifier
                .fillMaxWidth()
//                .weight(1f),
            ) {
                Row (
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ){
                    Column (
                        modifier = Modifier.weight(1f) // Ensures second column takes 50% of width
                    ) {
                        SensorDataDisplay(
                            label = "PM2.5",
                            value = "${data?.pm25level?.let { (it * 10).roundToInt() / 10.0 } ?: "--"}",
                            unit = "µg/m³"
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SensorDataDisplay(
                            label = "Temp",
                            value = "${data?.temperature?.let { (it * 10).roundToInt() / 10.0 }}",
                            unit = "°C"
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column (
                        modifier = Modifier.weight(1f) // Ensures second column takes 50% of width
                    ) {
                        SensorDataDisplay(
                            label = "CO",
                            value = "${data?.coLevel?.let { (it * 10).roundToInt() / 10.0 }}",
                            unit = "ppm"
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SensorDataDisplay(
                            label = "Hum",
                            value = "${data?.humidity?.let { (it * 10).roundToInt() / 10.0 }}",
                            unit = "%"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
//                    label = { Text("Description") },
                placeholder = { Text(text = "Enter descriptions here") },
                maxLines = 5,
                minLines = 5,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { openAlertDialog.value = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("End Session")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    when {
        openAlertDialog.value -> {
            EndSessionConfirmAlertDialog(
                onConfirm = onEndSessionConfirmed,
                onDismiss = { openAlertDialog.value = false }
            )
        }
    }
}

@Composable
fun SensorDataDisplay(
    value: String,
    label: String,
    unit: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Left: Large Number
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Right,
                overflow = TextOverflow.Clip,
                maxLines = 1
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Label & Unit
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(0.7f)
            ) {
                Text(text = label, fontSize = 16.sp, color = Color.Gray)
                Text(text = unit, fontSize = 14.sp, color = Color.DarkGray)
            }
        }
    }
}

private fun formatElapsedTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format(Locale.US,"%02d:%02d:%02d", hours, minutes, secs)
}

private fun formatLocation(latitude: Double, longitude: Double): String {
    return String.format(Locale.US, "(%.4f, %.4f)", latitude, longitude)
}

@Composable
private fun EndSessionConfirmAlertDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        title = { Text("End session") },
        text = { Text("Are you sure you want to end this session?") },
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Preview
@Composable
fun SensorDataDisplayPreview() {
    Box(modifier = Modifier
        .height(300.dp)
        .width(300.dp)
        .padding(40.dp),
        contentAlignment = Alignment.Center) {
        SensorDataDisplay("73", "PM2.5", "ug/m3")
    }
}

@Preview(showBackground = true)
@Composable
fun DataMonitorScreenPreview() {
    // Mock Location
    val mockLocation = Location("mockProvider").apply {
        latitude = 37.7749  // San Francisco latitude
        longitude = -122.4194 // San Francisco longitude
        altitude = 15.0
        accuracy = 5f
    }

    val dataEntry = generateFakeDataEntries(1)[0]

    DataMonitorScreen(dataEntry.copy(pm25level = 8000.0f), 1742088746000L, mockLocation, {})
}

@Preview(showBackground = true)
@Composable
fun DataMonitorScreenPreviewNull() {
    // Mock Location
    val mockLocation = Location("mockProvider").apply {
        latitude = 37.7749  // San Francisco latitude
        longitude = -122.4194 // San Francisco longitude
        altitude = 15.0
        accuracy = 5f
    }

    val dataEntry = generateFakeDataEntries(1)[0]

    DataMonitorScreen(dataEntry.copy(
        pm25level = null,
        coLevel = null,
        temperature = null,
        humidity = null
    ), 1742088746000L, mockLocation, {})
}