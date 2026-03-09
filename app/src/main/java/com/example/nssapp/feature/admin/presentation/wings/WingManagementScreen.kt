package com.example.nssapp.feature.admin.presentation.wings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nssapp.core.domain.model.Admin
import com.example.nssapp.core.domain.model.Wing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WingManagementScreen(
    viewModel: WingManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var showDialog by remember { mutableStateOf(false) }
    var wingToEdit by remember { mutableStateOf<Wing?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Wings") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                wingToEdit = null
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Wing")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is WingListUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is WingListUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is WingListUiState.Success -> {
                    if (state.wings.isEmpty()) {
                        Text(
                            text = "No wings found.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.wings) { wing ->
                                WingItem(
                                    wing = wing,
                                    onEdit = {
                                        wingToEdit = wing
                                        showDialog = true
                                    },
                                    onDelete = { viewModel.deleteWing(wing.id) }
                                )
                            }
                        }
                    }
                }
            }

            if (showDialog && uiState is WingListUiState.Success) {
                WingDialog(
                    wing = wingToEdit,
                    admins = (uiState as WingListUiState.Success).admins,
                    onDismiss = { showDialog = false },
                    onConfirm = { name, maxEnrollment, adminIds ->
                        if (wingToEdit == null) {
                            viewModel.addWing(name, maxEnrollment, adminIds)
                        } else {
                            viewModel.updateWing(wingToEdit!!.copy(name = name, maxEnrollment = maxEnrollment), adminIds)
                        }
                        showDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun WingItem(
    wing: Wing,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = wing.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = wing.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Max Enrollment: ${wing.maxEnrollment}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Wing", tint = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Wing", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WingDialog(
    wing: Wing?,
    admins: List<Admin>,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, List<String>) -> Unit
) {
    var name by remember { mutableStateOf(wing?.name ?: "") }
    var maxEnrollment by remember { mutableStateOf(wing?.maxEnrollment?.toString() ?: "") }
    var selectedAdminIds by remember { mutableStateOf(wing?.adminIds?.toSet() ?: emptySet()) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (wing == null) "Add Wing" else "Edit Wing") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Wing Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = maxEnrollment,
                    onValueChange = { maxEnrollment = it },
                    label = { Text("Max Enrollment") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("Assigned Admins (Coordinators/Managers)", style = MaterialTheme.typography.titleSmall)
                
                // Simple list of admins with checkboxes
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp).fillMaxWidth()) {
                    items(admins) { admin ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAdminIds = if (selectedAdminIds.contains(admin.id)) {
                                        selectedAdminIds - admin.id
                                    } else {
                                        selectedAdminIds + admin.id
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = selectedAdminIds.contains(admin.id),
                                onCheckedChange = { isChecked ->
                                    selectedAdminIds = if (isChecked) {
                                        selectedAdminIds + admin.id
                                    } else {
                                        selectedAdminIds - admin.id
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = admin.name, style = MaterialTheme.typography.bodyMedium)
                                Text(text = admin.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (isError) {
                    Text(
                        text = "Please enter valid data.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val enroll = maxEnrollment.toIntOrNull()
                    if (name.isNotBlank() && enroll != null && enroll > 0) {
                        onConfirm(name.trim(), enroll, selectedAdminIds.toList())
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
