package com.example.nssapp.feature.admin.presentation.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.core.domain.model.Wing
import com.example.nssapp.feature.admin.presentation.events.components.EventFormDialog
import java.text.SimpleDateFormat // Formatting
import java.util.Date
import java.util.Locale
import java.util.Calendar
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    onEventClick: (String) -> Unit,
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
                            EventItem(event, onClick = { onEventClick(event.id) })
                        }
                    }
                    
                    if (showAddEventDialog) {
                        EventFormDialog(
                            wings = state.wings,
                            onDismiss = { showAddEventDialog = false },
                            onConfirm = { title, type, date, startTime, endTime, posHours, negHours, mandatory, targetWings, mandatoryWings ->
                                viewModel.addEvent(title, type, date, startTime, endTime, posHours, negHours, mandatory, targetWings, mandatoryWings)
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
fun EventItem(event: Event, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
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
            
            // Show status in list too
            Text(text = "Status: ${event.status}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}


