package com.louislu.pennbioinformatics.screen.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.louislu.pennbioinformatics.domain.model.Session
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.random.Random

@Composable
fun SessionHistoryScreenRoot(
    navigateToEntries: (Long) -> Unit,
    onBackPressed: () -> Unit
) {
    val viewModel: SessionHistoryViewModel = hiltViewModel()
    val sessions by viewModel.sessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    SessionHistoryScreen(
        sessions = sessions,
        onSelect = { sessionId ->
            navigateToEntries(sessionId)
        },
        isLoading = isLoading, // or use a loading state if you add one
        onBackClicked = onBackPressed
    )
}


@Composable
fun SessionHistoryScreen(
    sessions: List<Session>,
    onSelect: (Long) -> Unit,
    isLoading: Boolean,
    onBackClicked: () -> Unit
) {
    var selectedSession by remember { mutableStateOf<Session?>(null) }

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
                Text("Past Sessions", style = MaterialTheme.typography.titleLarge)
                Text("Tap to view session detail", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            }
            if (sessions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessions) { session ->
                        val isSelected = selectedSession == session

                        ListItem(
                            headlineContent = { Text(
                                session.title,
                                style = MaterialTheme.typography.headlineSmall
                            ) },
                            overlineContent = { Text(
                                "Start time: ${formatEpochMillis(session.startTimestamp)}",
                                style = MaterialTheme.typography.bodySmall
                            ) },
//                            supportingContent = { Text(
//                                session.description ?: "",
//                                maxLines = 2,
//                                overflow = TextOverflow.Ellipsis,
//                                style = MaterialTheme.typography.bodySmall // or labelMedium
//                            ) },
                            colors = ListItemDefaults.colors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSession = session
                                    session.serverId?.let {
                                        onSelect(it)
                                    } ?: Timber.e("Server ID shouldn't be null")
                                }
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
                    Text("No session found (try connecting to the internet or creating new sessions)", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom Buttons

            OutlinedButton(
                onClick = onBackClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun formatEpochMillis(epochMillis: Long): String {
    val instant = Instant.ofEpochMilli(epochMillis)
    val zonedDateTime = instant.atZone(ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
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
fun SessionHistoryScreenPreview() {
    val sessions = generateMockSessions(10)
    SessionHistoryScreen(sessions = sessions, {}, false, {})
}