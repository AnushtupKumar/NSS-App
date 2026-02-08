package com.example.nssapp.feature.admin.presentation.students

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nssapp.core.domain.model.Student
import com.example.nssapp.core.domain.model.Wing
import com.example.nssapp.feature.admin.presentation.students.components.StudentFormDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentListScreen(
    viewModel: AdminStudentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedWings by viewModel.selectedWings.collectAsState()

    var showAddStudentDialog by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<Student?>(null) }
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
                        selectedWingIds = selectedWings,
                        onWingToggle = viewModel::toggleWingFilter,
                        onClearFilters = viewModel::clearFilters
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyColumn {
                        items(state.students) { student ->
                            StudentItem(
                                student = student,
                                onDelete = { viewModel.deleteStudent(student.id) },
                                onEdit = { editingStudent = student }
                            )
                        }
                    }
                    
                    if (showAddStudentDialog || editingStudent != null) {
                        StudentFormDialog(
                            initialStudent = editingStudent,
                            wings = state.wings,
                            onDismiss = { 
                                showAddStudentDialog = false 
                                editingStudent = null
                            },
                            onConfirm = { name, email, roll, wingIds, password ->
                                if (editingStudent != null) {
                                    viewModel.updateStudent(editingStudent!!.copy(
                                        name = name, 
                                        email = email, 
                                        roll = roll, 
                                        enrolledWings = wingIds, 
                                        password = password
                                    ))
                                } else {
                                    viewModel.addStudent(name, email, roll, wingIds, password)
                                }
                                showAddStudentDialog = false
                                editingStudent = null
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
    selectedWingIds: Set<String>,
    onWingToggle: (String) -> Unit,
    onClearFilters: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selectedWingIds.isEmpty(),
            onClick = onClearFilters,
            label = { Text("All") }
        )
        wings.forEach { wing ->
            FilterChip(
                selected = selectedWingIds.contains(wing.id),
                onClick = { onWingToggle(wing.id) },
                label = { Text(wing.name) },
                leadingIcon = if (selectedWingIds.contains(wing.id)) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

@Composable
fun StudentItem(
    student: Student,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = student.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "Roll: ${student.roll}", style = MaterialTheme.typography.bodyMedium)
                Text(text = student.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (student.password.isNotEmpty()) {
                    Text(text = "Pass: ${student.password}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
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
