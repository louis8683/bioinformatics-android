package com.louislu.pennbioinformatics.screen.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.louislu.pennbioinformatics.R
import com.louislu.pennbioinformatics.auth.AuthViewModel
import com.louislu.pennbioinformatics.domain.model.UserInfo
import timber.log.Timber

@Composable
fun SelectGroupScreenRoot(
    authViewModel: AuthViewModel,
    navigateToPermission: () -> Unit,
    fromLogin: Boolean
) {
    val userInfo by authViewModel.userInfo.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.getUserInfo()
    }

    LaunchedEffect(userInfo) {
        userInfo?.let {
            if (fromLogin && it.schoolName != null && it.className != null && it.groupName != null) {
                navigateToPermission()
            }
        }
    }

    SelectGroupScreen(userInfo) { schoolName, className, groupName ->
        authViewModel.updateUserInfo(schoolName, className, groupName)
        // TODO: instead of navigating after this call, use a event state to signal a successful or failure of update
        navigateToPermission()
    }
}

@Composable
fun SelectGroupScreen(
    userInfo: UserInfo?,
    onConfirmClicked: (String, String, String) -> Unit
) {
    var missingFields by remember { mutableStateOf(false) }
    var selectedSchool by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("") }

    val optionsSchool = listOf("Philadelphia High School For Girls") // Add more schools if needed
    val optionsClass = listOf(
        "Class A",
        "Class B",
        "Class C",
        "Class D",
        "Class E",
    )
    val optionsGroup = listOf(
        "Group 1",
        "Group 2",
        "Group 3",
        "Group 4",
        "Group 5",
        "Group 6",
        "Group 7",
        "Group 8",
        "Group 9",
        "Group 10",
        "None"
    )

    LaunchedEffect(userInfo) {
        userInfo?.schoolName?.let {
            if (optionsSchool.contains(it)) selectedSchool = it
        }
        userInfo?.className?.let {
            if (optionsClass.contains(it)) selectedClass = it
        }
        userInfo?.groupName?.let {
            if (optionsGroup.contains(it)) selectedGroup= it
        }

    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(text = stringResource(R.string.the_bioinformatics_app), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Please select your group from the dropdown menu", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(16.dp))

                MyDropdownMenu("School", optionsSchool, { selectedSchool = it }, selectedSchool)
                Spacer(modifier = Modifier.height(16.dp))

                MyDropdownMenu("Class", optionsClass, { selectedClass = it }, selectedClass)
                Spacer(modifier = Modifier.height(16.dp))

                MyDropdownMenu("Group", optionsGroup, { selectedGroup = it }, selectedGroup)
                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    Timber.i("$selectedSchool, $selectedClass, $selectedGroup")
                    if (selectedSchool.isEmpty() || selectedClass.isEmpty() || selectedGroup.isEmpty()) {
                        missingFields = true
                    }
                    else {
                        onConfirmClicked(selectedSchool, selectedClass, selectedGroup)
                    }
                }) {
                    Text("Confirm")
                }

                if (missingFields) {
                    Text("Please select all fields", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MyDropdownMenu(
    label: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    selectedText: String,
    filterInput: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.padding(32.dp, 0.dp)
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {
                inputText = it
            },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth(),
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            readOnly = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                keyboardController?.hide()
            }
        ) {
            options.forEach { option ->
                if (!filterInput || option.contains(inputText, ignoreCase = true) ) {
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            inputText = option
                            onSelect(option)
                            expanded = false
                            keyboardController?.hide()
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun SelectGroupScreenPreview() {
    SelectGroupScreen(null, {a, b, c -> })
}