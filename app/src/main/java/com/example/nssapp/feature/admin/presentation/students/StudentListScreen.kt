package com.example.nssapp.feature.admin.presentation.students

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nssapp.core.domain.model.Student
import com.example.nssapp.core.domain.model.Wing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentListScreen(
    viewModel: AdminStudentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedWing by viewModel.selectedWing.collectAsState()

    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showAddWingDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddStudentDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Student")
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Manage Students") },
                actions = {
                    TextButton(onClick = { showAddWingDialog = true }) {
                        Text("Add Wing")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            when (val state = uiState) {
                is StudentUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is StudentUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is StudentUiState.Success -> {
                    // Filter Row
                    WingFilterRow(
                        wings = state.wings,
                        selectedWingId = selectedWing,
                        onWingSelected = viewModel::selectWing
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyColumn {
                        items(state.students) { student ->
                            StudentItem(student)
                        }
                    }
                    
                    if (showAddStudentDialog) {
                        AddStudentDialog(
                            wings = state.wings,
                            onDismiss = { showAddStudentDialog = false },
                            onConfirm = { name, email, roll, wingId ->
                                viewModel.addStudent(name, email, roll, wingId)
                                showAddStudentDialog = false
                            }
                        )
                    }
                    
                    if (showAddWingDialog) {
                        AddWingDialog(
                            onDismiss = { showAddWingDialog = false },
                            onConfirm = { name ->
                                viewModel.addWing(name)
                                showAddWingDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WingFilterRow(
    wings: List<Wing>,
    selectedWingId: String?,
    onWingSelected: (String?) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selectedWingId == null,
            onClick = { onWingSelected(null) },
            label = { Text("All") }
        )
        wings.forEach { wing ->
            FilterChip(
                selected = selectedWingId == wing.id,
                onClick = { onWingSelected(wing.id) },
                label = { Text(wing.name) }
            )
        }
    }
}

@Composable
fun StudentItem(student: Student) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = student.name, style = MaterialTheme.typography.titleMedium)
            Text(text = "Roll: ${student.roll}", style = MaterialTheme.typography.bodyMedium)
            Text(text = student.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AddStudentDialog(
    wings: List<Wing>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var roll by remember { mutableStateOf("") }
    var selectedWingId by remember { mutableStateOf(if (wings.isNotEmpty()) wings[0].id else "") }
    
    // Simple dropdown or radio logic for dialog. For now, specific Wing ID selection is tricky in simple dialog.
    // Let's assume user picks from a simplified list or just default first one for MVP/Proto.
    // Actually, let's make a simple dropdown replacement: A Row of buttons if few, or just text input?
    // Let's us a simple DropdownMenu logic or just iterate radio buttons.
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Student") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
                OutlinedTextField(value = roll, onValueChange = { roll = it }, label = { Text("Roll No") })
                Spacer(modifier = Modifier.height(8.dp))
                Text("Select Wing:")
                val expanded = remember { mutableStateOf(false) }
                Box {
                    Button(onClick = { expanded.value = true }) {
                        Text(wings.find { it.id == selectedWingId }?.name ?: "Select Wing")
                    }
                    DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
                        wings.forEach { wing ->
                            DropdownMenuItem(
                                text = { Text(wing.name) },
                                onClick = {
                                    selectedWingId = wing.id
                                    expanded.value = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (name.isNotBlank() && email.isNotBlank() && roll.isNotBlank() && selectedWingId.isNotBlank()) {
                         onConfirm(name, email, roll, selectedWingId) 
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddWingDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Wing") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Wing Name") })
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
