package com.louislu.pennbioinformatics.screen.monitor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.LocationServices
import com.louislu.pennbioinformatics.domain.model.DataEntry
import com.louislu.pennbioinformatics.domain.model.Session
import com.louislu.pennbioinformatics.domain.model.generateFakeDataEntries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DataMonitorScreenRoot(
    dataMonitorViewModel: DataMonitorViewModel,
    navigateToMenu: () -> Unit
) {
    val dataEntry by dataMonitorViewModel.latestDataEntry.collectAsState()
    val session by dataMonitorViewModel.currentSession.collectAsState()

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var location by remember { mutableStateOf<Location?>(null) }

    val isUpdating by dataMonitorViewModel.isUpdating.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val isConnected by dataMonitorViewModel.isConnected.collectAsState()

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation                .addOnSuccessListener { location = it }
        } else {
            // TODO: handle the case when permission is not granted (should be a rare case)
        }

        dataMonitorViewModel.startSession()
    }

    LaunchedEffect(isConnected) {
        // TODO: indicate that a connection lost caused this
        if (!isConnected) {
            dataMonitorViewModel.stopSession()
            dataMonitorViewModel.syncPendingAndNavigate()
        }
    }

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "monitor") {

        composable(route = "monitor") {
            LaunchedEffect(Unit) {
                dataMonitorViewModel.navigateToSessionEnded.collect {
                    navController.navigate("sessionEnded?connectionLost=${!isConnected}")
                }
            }

            DataMonitorScreen(
                data = dataEntry,
                session = session,
                onEndSessionConfirmed = {
                    dataMonitorViewModel.stopSession()
                    dataMonitorViewModel.syncPendingAndNavigate()
                    // TODO: add a loading indicator
                },
                onTitleUpdated = { dataMonitorViewModel.updateSession(title = it) },
                onDescriptionUpdated = { dataMonitorViewModel.updateSession(description = it) }
            )
        }
        composable(route = "sessionEnded?connectionLost={connectionLost}") { backStackEntry ->
            val connectionLost = backStackEntry.arguments?.getString("connectionLost")?.toBoolean() == true

            SessionEndedScreen(
                initialTitle = session?.title ?: "",
                initialDescription = session?.description ?: "",
                onUpdateClicked = { title, description ->
                    coroutineScope.launch {
                        dataMonitorViewModel.updateSession(title, description)
                        dataMonitorViewModel.isUpdating.first{ !it }
                        navigateToMenu()
                    }
                },
                isUpdating = isUpdating,
                connectionLost = connectionLost
            )
        }
    }
}

@Composable
fun DataMonitorScreen(
    data: DataEntry?,
    session: Session?,
    onEndSessionConfirmed: () -> Unit,
    onTitleUpdated: (String) -> Unit,
    onDescriptionUpdated: (String) -> Unit
    // TODO: what if internet is lost halfway? can "createSession" update the session?
) {
    var secondsElapsed by remember { mutableLongStateOf(0) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val openAlertDialog = remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(session) {
        while (isActive) {
            val now = System.currentTimeMillis()
            secondsElapsed = session?.let { (now - session.startTimestamp) / 1000 } ?: 0L
            delay(1000L)
        }
    }
    LaunchedEffect(title) { onTitleUpdated(title) }
    LaunchedEffect(description) { onDescriptionUpdated(description) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                },
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Texts

//            Spacer(modifier = Modifier.height(32.dp))
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
//                Text("The Bioinformatics App", style = MaterialTheme.typography.titleLarge) // TODO: Bold
                Text("Session running...", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Duration: ${formatElapsedTime(secondsElapsed)}", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Location: ${formatLocation(data?.latitude ?: Double.MIN_VALUE, data?.longitude ?: Double.MIN_VALUE)}", style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                            value = "${data?.pm25level?.let {
                                (it * 10).roundToInt() / 10.0 
                            } ?: "N/A"}",
                            unit = "µg/m³"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SensorDataDisplay(
                            label = "Temp",
                            value = "${data?.temperature?.let { 
                                (it * 10).roundToInt() / 10.0 
                            } ?: "N/A"}",
                            unit = "°C"
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column (
                        modifier = Modifier.weight(1f) // Ensures second column takes 50% of width
                    ) {
                        SensorDataDisplay(
                            label = "CO",
                            value = "${data?.coLevel?.let {
                                (it * 10).roundToInt() / 10.0 
                            } ?: "N/A"}",
                            unit = "ppm"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SensorDataDisplay(
                            label = "Hum",
                            value = "${data?.humidity?.let {
                                (it * 1000).roundToInt() / 10.0 
                            } ?: "N/A"}",
                            unit = "%"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))


            TitleTextField(
                title = title,
                onValueChange = { title = it },
                focusManager = focusManager,
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            DescriptionTextField(
                description = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { openAlertDialog.value = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("End Session")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    when {
        openAlertDialog.value -> {
            EndSessionConfirmAlertDialog(
                onConfirm = {
                    openAlertDialog.value = false
                    onEndSessionConfirmed()
                },
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
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
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

            Spacer(modifier = Modifier.width(4.dp))

            // Right: Label & Unit
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(0.7f)
            ) {
                Text(text = label, fontSize = 12.sp, color = Color.Gray)
                Text(text = unit, fontSize = 11.sp, color = Color.DarkGray)
            }
        }
    }
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

@Composable
fun TitleTextField(
    title: String,
    onValueChange: (String) -> Unit,
    focusManager: FocusManager,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = title,
        onValueChange = onValueChange,
        placeholder = { Text(text = "Enter session title here") },
        label = { Text("Session Title") },
        maxLines = 1,
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = {
            focusManager.clearFocus()
        }),
        modifier = modifier
    )
}

@Composable
fun DescriptionTextField(
    description: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = description,
        onValueChange = onValueChange,
        placeholder = { Text(text = "Enter observations here") },
        label = { Text("Observation") },
        modifier = modifier
    )
}

@Composable
fun QuitAlertDialog(
    onDismiss: () -> Unit,
    onConfirmClicked: () -> Unit,
    onDismissClicked: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quit App") },
        text = { Text("Are you sure you want to quit the app?") },
        confirmButton = {
            TextButton(onClick = onConfirmClicked) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissClicked) {
                Text("Cancel")
            }
        }
    )
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

private fun mockSession(): Session {
    return Session(
        localId = 1L,
        serverId = 100L,
        userId = "test-user",
        groupName = "test-group",
        className = "test-class",
        schoolName = "test-school",
        deviceName = "AA:BB:CC:DD:EE:FF",
        startTimestamp = 1742088746000L, // Static timestamp for consistency
        endTimestamp = null, // Session is ongoing
        title = "test-title",
        description = "Mock Bio Data Session",
        pendingUpload = false
    )
}

@Preview(showBackground = true)
@Composable
fun DataMonitorScreenPreview() {

    val dataEntry = generateFakeDataEntries(1)[0]


    DataMonitorScreen(dataEntry.copy(pm25level = 8000.0f), mockSession(), {}, {}, {})
}

@Preview(
    showBackground = true,
    widthDp = 320,
    heightDp = 568,
    name = "Small Screen Preview"
)
@Composable
fun DataMonitorScreenSmallPreview() {
    val dataEntry = generateFakeDataEntries(1)[0]

    DataMonitorScreen(
        data = dataEntry.copy(
            pm25level = 800.0f,
            humidity = 0.74f
        ),
        session = mockSession(),
        onEndSessionConfirmed = {},
        onTitleUpdated = {},
        onDescriptionUpdated = {}
    )
}

@Preview(showBackground = true)
@Composable
fun DataMonitorScreenPreviewNull() {

    val dataEntry = generateFakeDataEntries(1)[0]

    DataMonitorScreen(dataEntry.copy(
        pm25level = null,
        coLevel = null,
        temperature = null,
        humidity = null
    ), mockSession(), {}, {}, {})
}