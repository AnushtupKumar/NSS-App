package com.example.nssapp.feature.student.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nssapp.core.domain.model.Event
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    onProfileClick: () -> Unit,
    onScanClick: () -> Unit,
    viewModel: StudentHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NSS Attendance") },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onScanClick) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is StudentHomeUiState.Loading -> {
                     Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is StudentHomeUiState.Error -> {
                     Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is StudentHomeUiState.Success -> {
                    // Header with Stats
                    StatsHeader(state.student.name, state.attendancePercentage)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Upcoming Events",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(state.events.filter { it.date >= System.currentTimeMillis() }) { event ->
                            StudentEventItem(event, isAttended = state.attendedEventIds.contains(event.id))
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Past Events",
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }

                         items(state.events.filter { it.date < System.currentTimeMillis() }.sortedByDescending { it.date }) { event ->
                            StudentEventItem(event, isAttended = state.attendedEventIds.contains(event.id))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsHeader(name: String, percentage: Float) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Welcome,", style = MaterialTheme.typography.titleMedium)
                Text(name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { percentage / 100f },
                    modifier = Modifier.size(80.dp),
                    strokeWidth = 8.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    text = "${percentage.toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StudentEventItem(event: Event, isAttended: Boolean) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = event.title, style = MaterialTheme.typography.titleMedium)
                if (isAttended) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Attended", tint = Color(0xFF4CAF50))
                }
            }
            Text(text = "Type: ${event.type}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Date: ${dateFormat.format(Date(event.date))}", style = MaterialTheme.typography.bodySmall)
            
            if (event.mandatory) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "MANDATORY", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}
