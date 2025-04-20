package com.louislu.pennbioinformatics.screen.history

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.louislu.pennbioinformatics.domain.model.DataEntry
import com.louislu.pennbioinformatics.domain.model.Session
import com.louislu.pennbioinformatics.domain.model.generateFakeDataEntries
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun EntryHistoryScreenRoot(
    entryHistoryViewModel: EntryHistoryViewModel,
    onBackPressed: () -> Unit
) {
    val session by entryHistoryViewModel.session.collectAsState()
    val entries by entryHistoryViewModel.entries.collectAsState()
    val isLoading by entryHistoryViewModel.isLoading.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            entryHistoryViewModel.exportCsvCompat(context, filename = session?.let {
                "${it.startTimestamp}-${it.title}.csv"
            } ?: "export.csv")
        } else {
            Timber.e("Storage permission denied")
        }
    }

    EntryHistoryScreen(
        entries = entries,
        session = session,
        isLoading = isLoading, // or use a loading state if you add one
        onBackClicked = onBackPressed,
        onExportClicked = {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                entryHistoryViewModel.exportCsvCompat(context, filename = session?.let {
                    "${it.startTimestamp}-${it.title}.csv"
                } ?: "export.csv")
            }
        }
    )
}


@Composable
fun EntryHistoryScreen(
    entries: List<DataEntry>,
    session: Session?,
    isLoading: Boolean,
    onBackClicked: () -> Unit,
    onExportClicked: () -> Unit
) {
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

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp, 0.dp)
            ) {
//                Text("The Bioinformatics App", style = MaterialTheme.typography.titleLarge)
                Text(session?.title ?: "Entries from Session", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Date: ${
                    session?.let { formatEpochMillisToDate(it.startTimestamp) }
                }", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Column(horizontalAlignment = AbsoluteAlignment.Left) {
                    Text("1. Click \"Export\" to download", style = MaterialTheme.typography.labelLarge)
                    Text("2. Open the \"My Files\" app", style = MaterialTheme.typography.labelLarge)
                    Text("3. Go to the \"Download\" folder", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            }
            if (entries.isNotEmpty()) {
                DataEntryTableHeader()
                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries) { entry ->
                        DataEntryRow(entry)
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
                    Text("No entries found", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Buttons

            Button(
                onClick = onExportClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export CSV file to device")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onBackClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun DataEntryTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        TableCell(text = "Time",
            modifier = Modifier.weight(1.2f))
        TableCell(text = "PM2.5",
            modifier = Modifier.weight(1f))
        TableCell(text = "CO",
            modifier = Modifier.weight(1f))
        TableCell(text = "Temp",
            modifier = Modifier.weight(1f))
        TableCell(text = "Humidity",
            modifier = Modifier.weight(1f))
    }
}

@Composable
fun DataEntryRow(entry: DataEntry, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TableCell(
            text = formatEpochMillisToHHMMSS(entry.timestamp),
            modifier = Modifier.weight(1.2f))
        TableCell(
            text = "${entry.pm25level?.let { (it * 10).roundToInt() / 10.0 } ?: "-"}",
            modifier = Modifier.weight(1f))
        TableCell(
            text = "${entry.coLevel?.let { (it * 10).roundToInt() / 10.0 } ?: "-"}",
            modifier = Modifier.weight(1f))
        TableCell(
            text = "${entry.temperature?.let { (it * 10).roundToInt() / 10.0 } ?: "-"}",
            modifier = Modifier.weight(1f))
        TableCell(
            text = entry.humidity?.let {
                "${(it * 100).roundToInt()}%"
            } ?: "-",
            modifier = Modifier.weight(1f))
    }
}

@Composable
fun TableCell(text: String, modifier: Modifier) {
    Box (
        modifier = modifier
    ) {
        Text(
            text = text,
            modifier = Modifier
                .padding(horizontal = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


private fun formatEpochMillisToHHMMSS(epochMillis: Long): String {
    val instant = Instant.ofEpochMilli(epochMillis)
    val zonedDateTime = instant.atZone(ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    return formatter.format(zonedDateTime)
}

private fun formatEpochMillisToDate(epochMillis: Long): String {
    val instant = Instant.ofEpochMilli(epochMillis)
    val zonedDateTime = instant.atZone(ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    return formatter.format(zonedDateTime)
}

private fun loremIpsum(words: Int = 20): String {
    val base = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
    return base.split(" ").take(words).joinToString(" ")
}

private fun generateMockSessions(count: Int = 5): List<Session> {
    val now = System.currentTimeMillis()

    return List(count) { index ->
        val start = now - (index * 2 * 60 * 60 * 1000) // go back 2hr each
        val end = start + 60 * 60 * 1000 // 1 hour duration

        Session(
            localId = index.toLong(),
            serverId = if (index % 2 == 0) 1000L + index else null, // alternate pendingUpload
            userId = "mock-user-${index + 1}",
            groupName = if (index % 3 == 0) "Group ${index + 1}" else null,
            className = "Class ${(65 + index).toChar()}",
            schoolName = "Mock High School",
            deviceName = "Sensor-${index + 1}",
            startTimestamp = start,
            endTimestamp = end,
            title = "Session #${index + 1}",
            description = loremIpsum(Random.nextInt(1, 30)),
            pendingUpload = index % 2 != 0 // mark odd sessions as pending upload
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EntryHistoryScreenPreview() {
    EntryHistoryScreen(generateFakeDataEntries(50), null, false, {}, {})
}

@Preview(
    showBackground = true,
    widthDp = 320,
    heightDp = 568,
    name = "Small Screen Preview"
)
@Composable
fun EntryHistoryScreenSmallPreview() {
    val session = generateMockSessions(1)[0]
    EntryHistoryScreen(generateFakeDataEntries(50), session.copy(title = "My Session Title"), false, {}, {})
}