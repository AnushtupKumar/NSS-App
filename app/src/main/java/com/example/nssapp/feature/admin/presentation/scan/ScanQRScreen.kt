package com.example.nssapp.feature.admin.presentation.scan

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nssapp.core.domain.model.Event

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanQRScreen(
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val events by viewModel.activeEvents.collectAsState()
    
    var selectedEventId by remember { mutableStateOf("") }
    var rollNo by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    // Auto-select first event if available and none selected
    LaunchedEffect(events) {
        if (selectedEventId.isEmpty() && events.isNotEmpty()) {
            selectedEventId = events.first().id
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mark Attendance") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Event Selector
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = events.find { it.id == selectedEventId }?.title ?: "Select Event",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    events.forEach { event ->
                        DropdownMenuItem(
                            text = { Text(event.title) },
                            onClick = {
                                selectedEventId = event.id
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedTextField(
                value = rollNo,
                onValueChange = { rollNo = it },
                label = { Text("Student Roll No") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { 
                    if (selectedEventId.isNotEmpty() && rollNo.isNotBlank()) {
                         viewModel.markAttendance(selectedEventId, rollNo)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = uiState !is AttendanceUiState.Loading
            ) {
                if (uiState is AttendanceUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Mark Present")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Status Message
            when (val state = uiState) {
                is AttendanceUiState.Success -> {
                    Text(text = state.message, color = Color(0xFF4CAF50), style = MaterialTheme.typography.titleMedium)
                    // Clear input on success?
                    // rollNo = "" // Optional: Keep it to avoid typing again if checking same student? No, usually clear it.
                    // Let's clear it in a side effect.
                    LaunchedEffect(state) {
                        rollNo = ""
                    }
                }
                is AttendanceUiState.Error -> {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                }
                else -> {}
            }
        }
    }
}
