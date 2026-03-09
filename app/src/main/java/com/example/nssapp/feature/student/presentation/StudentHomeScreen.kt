package com.example.nssapp.feature.student.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nssapp.core.domain.model.AttendanceStatus
import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.core.domain.model.EventStatus
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.messaging.FirebaseMessaging

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    onProfileClick: () -> Unit,
    onScanClick: () -> Unit,
    viewModel: StudentHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("FCM", "Notification permission granted")
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is StudentHomeUiState.Success) {
            val state = uiState as StudentHomeUiState.Success
            FirebaseMessaging.getInstance().subscribeToTopic("events_updates")
            state.student.enrolledWings.forEach { wingId ->
                val safeTopicName = wingId.replace(Regex("[^a-zA-Z0-9-_.~%]+"), "_")
                FirebaseMessaging.getInstance().subscribeToTopic("wing_$safeTopicName")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("My Dashboard", style = MaterialTheme.typography.titleLarge)
                        if (uiState is StudentHomeUiState.Success) {
                            Text(
                                (uiState as StudentHomeUiState.Success).student.name, 
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onScanClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header Stats
                        DashboardStats(state.totalHours, state.attendancePercentage)
                        
                        // Event Calendar
                        var selectedDate by remember { mutableStateOf<java.time.LocalDate?>(null) }
                        
                        com.example.nssapp.feature.student.presentation.components.EventCalendar(
                            events = state.allEvents,
                            selectedDate = selectedDate,
                            onDateSelected = { date ->
                                selectedDate = if (selectedDate == date) null else date
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        )

                        // Category Tabs
                        CategoryFilterChips(
                            selectedCategory = state.selectedCategory,
                            onCategorySelected = { viewModel.setCategory(it) }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val eventsToShow = remember(state.filteredEvents, selectedDate) {
                            if (selectedDate == null) {
                                state.filteredEvents
                            } else {
                                state.filteredEvents.filter {
                                    java.time.Instant.ofEpochMilli(it.event.date)
                                        .atZone(java.time.ZoneId.systemDefault())
                                        .toLocalDate() == selectedDate
                                }
                            }
                        }
                        
                        if (eventsToShow.isEmpty()) {
                            EmptyState()
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(eventsToShow, key = { it.event.id }) { item ->
                                    val statusString = state.attendanceStatuses[item.event.id]
                                    
                                    StudentEventItem(
                                        event = item.event,
                                        status = statusString,
                                        matchingWings = item.matchingWings
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardStats(totalHours: Double, percentage: Float) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Total NSS Hours",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        String.format("%.1f", totalHours),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Attendance",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "${percentage.toInt()}%",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthSelector(selectedMonth: Int, onMonthSelected: (Int) -> Unit) {
    val months = DateFormatSymbols().months.filter { it.isNotEmpty() }
    val calendar = Calendar.getInstance()
    
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(months.indices.toList()) { index ->
            val isSelected = index == selectedMonth
            Surface(
                modifier = Modifier.clickable { onMonthSelected(index) },
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = months[index].take(3),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CategoryFilterChips(selectedCategory: EventCategory, onCategorySelected: (EventCategory) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(EventCategory.entries.toTypedArray()) { category ->
            val isSelected = category == selectedCategory
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = { 
                    val text = if (category == EventCategory.ABSENT) "Missed" else category.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                    Text(text) 
                },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun StudentEventItem(event: Event, status: String?, matchingWings: List<String> = emptyList()) {
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    
    val isAttended = status == AttendanceStatus.PRESENT.value
    val isPenalty = status == AttendanceStatus.PENALTY.value
    val isAbsent = status == AttendanceStatus.ABSENT.value
    val isUpcoming = status == null && event.date >= System.currentTimeMillis()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date Column
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dateFormat.format(Date(event.date)).split(" ")[0],
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = dateFormat.format(Date(event.date)).split(" ")[1],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule, 
                        contentDescription = null, 
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeFormat.format(Date(event.startTime)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Matching Wings
                if (matchingWings.isNotEmpty()) {
                    Text(
                        text = "Wings: ${matchingWings.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Hours Display
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                    Text(
                        text = "+${event.positiveHours} Hrs",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary, // Using primary since bright green might fail contrast in dark mode
                        fontWeight = FontWeight.Bold
                    )
                    if (event.mandatory) {
                        Text(
                            text = "-${event.negativeHours} Hrs",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Badges
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isAttended) {
                        StatusBadge("Attended", MaterialTheme.colorScheme.primary)
                    } else if (isPenalty) {
                        StatusBadge("Penalized", MaterialTheme.colorScheme.error)
                    } else if (isAbsent) {
                        StatusBadge("Missed", MaterialTheme.colorScheme.outline)
                    } else if (isUpcoming) {
                        StatusBadge("Upcoming", MaterialTheme.colorScheme.secondary)
                    }
                    
                    if (event.mandatory) {
                        StatusBadge("Mandatory", MaterialTheme.colorScheme.tertiary)
                    }
                }
            }

            if (isAttended) {
                Icon(
                    Icons.Default.Verified, 
                    contentDescription = "Verified", 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = CircleShape,
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun HoursBadge(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.EventBusy, 
            contentDescription = null, 
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No events found for this selection",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
