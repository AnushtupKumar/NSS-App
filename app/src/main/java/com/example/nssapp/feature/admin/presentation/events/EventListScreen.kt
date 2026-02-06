package com.example.nssapp.feature.admin.presentation.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.core.domain.model.Wing
import java.text.SimpleDateFormat // Formatting
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    viewModel: AdminEventViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddEventDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddEventDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
        },
        topBar = {
            TopAppBar(title = { Text("Events") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            when (val state = uiState) {
                is EventUiState.Loading -> {
                     Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is EventUiState.Error -> {
                     Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is EventUiState.Success -> {
                    LazyColumn {
                        items(state.events) { event ->
                            EventItem(event)
                        }
                    }
                    
                    if (showAddEventDialog) {
                        AddEventDialog(
                            wings = state.wings,
                            onDismiss = { showAddEventDialog = false },
                            onConfirm = { title, type, mandatory, targetWings ->
                                viewModel.addEvent(title, type, mandatory, targetWings)
                                showAddEventDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EventItem(event: Event) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = event.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (event.mandatory) {
                    Text(text = "Mandatory", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(text = "Type: ${event.type}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Date: ${dateFormat.format(Date(event.date))}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun AddEventDialog(
    wings: List<Wing>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean, List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var mandatory by remember { mutableStateOf(false) }
    // Multi-select for wings? Simplification: Just one wing or "All" logic for now? 
    // Or just a checkbox list.
    // Let's do a simple implementation: 
    // Select one primary wing or none (meaning all?).
    // Schema says list of strings.
    // Let's allow selecting multiple.
    
    val selectedWings = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Event") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type (e.g. Camp)") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = mandatory, onCheckedChange = { mandatory = it })
                    Text("Mandatory")
                }
                Text("Target Wings:", style = MaterialTheme.typography.titleSmall)
                wings.forEach { wing ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selectedWings.contains(wing.id),
                            onCheckedChange = { checked ->
                                if (checked) selectedWings.add(wing.id) else selectedWings.remove(wing.id)
                            }
                        )
                        Text(wing.name)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (title.isNotBlank()) {
                         onConfirm(title, type, mandatory, selectedWings.toList())
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
