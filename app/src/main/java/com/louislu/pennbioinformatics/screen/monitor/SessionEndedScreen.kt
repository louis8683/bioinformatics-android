package com.louislu.pennbioinformatics.screen.monitor

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.louislu.pennbioinformatics.R

@Composable
fun SessionEndedScreen(
    initialTitle: String,
    initialDescription: String,
    onUpdateClicked: (String, String) -> Unit,
    isUpdating: Boolean
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    val focusManager = LocalFocusManager.current

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .pointerInput(Unit) {
                        detectTapGestures( onTap = {
                            focusManager.clearFocus()
                        })
                    }
            ) {
                Text(text = stringResource(R.string.the_bioinformatics_app), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Session ended", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Finalize your observation", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(16.dp))

                TitleTextField(
                    title = title,
                    onValueChange = { title = it },
                    focusManager = focusManager,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                DescriptionTextField(
                    description = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    onUpdateClicked(title, description)
                }) {
                    if (isUpdating) CircularProgressIndicator()
                    else Text("Done")
                }
            }
        }
    }
}

@Preview
@Composable
fun SessionEndedScreenPreview() {
    SessionEndedScreen("some title", "some description", { str1, str2 -> }, false)
}