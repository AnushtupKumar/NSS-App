package com.example.nssapp.feature.admin.presentation.students

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddStudentDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Student")
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Manage Students") }
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadData() }) {
                                Text("Retry")
                            }
                        }
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
                    
                    if (state.students.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No students found", style = MaterialTheme.typography.titleMedium)
                                Text("Tap '+' to register a new student!", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else {
                        LazyColumn {
                            items(state.students) { student ->
                                 StudentItem(
                                    student = student,
                                    wings = state.wings,
                                    onDelete = { viewModel.deleteStudent(student.id) },
                                    onEdit = { editingStudent = student }
                                )
                            }
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
                            onConfirm = { name, email, roll, wingIds ->
                                if (editingStudent != null) {
                                    viewModel.updateStudent(editingStudent!!.copy(
                                        name = name, 
                                        email = email, 
                                        roll = roll, 
                                        enrolledWings = wingIds
                                    ))
                                } else {
                                    viewModel.addStudent(name, email, roll, wingIds, "temp@1234")
                                }
                                showAddStudentDialog = false
                                editingStudent = null
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
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(1){
            FilterChip(
            selected = selectedWingIds.isEmpty(),
            onClick = onClearFilters,
            label = { Text("All") }
        )
        }
        items(wings){ wing ->
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
    wings: List<Wing>,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val studentWings = wings.filter { student.enrolledWings.contains(it.id) }.joinToString(", ") { it.name }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name, 
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person, 
                        contentDescription = null, 
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Roll: ${student.roll}", 
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Wings: ${studentWings.ifEmpty { "None" }}", 
                    style = MaterialTheme.typography.labelMedium, 
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = student.email, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit, 
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete, 
                        contentDescription = "Delete", 
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}




