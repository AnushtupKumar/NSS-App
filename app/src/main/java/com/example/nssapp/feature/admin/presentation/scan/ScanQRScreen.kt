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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Auto-select first event if available and none selected
    LaunchedEffect(events) {
        if (selectedEventId.isEmpty() && events.isNotEmpty()) {
            selectedEventId = events.first().id
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mark Attendance") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Scan or Enter Roll No", 
                        style = MaterialTheme.typography.titleLarge, 
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

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
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            events.forEach { event ->
                                DropdownMenuItem(
                                    text = { Text(event.title, style = MaterialTheme.typography.bodyLarge) },
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
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { 
                            if (selectedEventId.isNotEmpty() && rollNo.isNotBlank()) {
                                 viewModel.markAttendance(selectedEventId, rollNo)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        enabled = uiState !is AttendanceUiState.Loading
                    ) {
                        if (uiState is AttendanceUiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Mark Present", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            
            // Status Message logic moved to LaunchedEffect for Snackbars
            LaunchedEffect(uiState) {
                when (val state = uiState) {
                    is AttendanceUiState.Success -> {
                        snackbarHostState.showSnackbar(state.message)
                        rollNo = ""
                        viewModel.resetState()
                    }
                    is AttendanceUiState.Error -> {
                        snackbarHostState.showSnackbar(state.message)
                        viewModel.resetState()
                    }
                    else -> {}
                }
            }
        }
    }
}
