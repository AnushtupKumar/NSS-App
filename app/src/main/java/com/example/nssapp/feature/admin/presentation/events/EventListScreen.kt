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
import androidx.compose.foundation.layout.width
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.core.domain.model.EventStatus
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(androidx.compose.material.icons.Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadData() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is EventUiState.Success -> {
                    if (state.events.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No events found", style = MaterialTheme.typography.titleMedium)
                                Text("Tap '+' to create one!", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else {
                        LazyColumn {
                            items(state.events) { event ->
                                EventItem(event, onClick = { onEventClick(event.id) })
                            }
                        }
                    }
                    
                    if (showAddEventDialog) {
                        EventFormDialog(
                            wings = state.wings,
                            onDismiss = { showAddEventDialog = false },
                            onConfirm = { title, description, date, startTime, endTime, posHours, negHours, mandatory, targetWings, mandatoryWings, studentsExcluded ->
                                viewModel.addEvent(title, description, date, startTime, endTime, posHours, negHours, mandatory, targetWings, mandatoryWings, studentsExcluded)
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween, 
                modifier = Modifier.fillMaxWidth(), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title, 
                    style = MaterialTheme.typography.titleMedium, 
                    color = MaterialTheme.colorScheme.onSurface, 
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DateRange, 
                    contentDescription = null, 
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = dateFormat.format(Date(event.date)), 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = event.description, 
                style = MaterialTheme.typography.bodyMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (event.status == EventStatus.ACTIVE.value) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = event.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (event.status == EventStatus.ACTIVE.value) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (event.mandatory) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "MANDATORY",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
