package com.example.nssapp.feature.admin.presentation.events.components

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.core.domain.model.Wing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormDialog(
    initialEvent: Event? = null,
    wings: List<Wing>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, Long, Long, Double, Double, Boolean, List<String>, List<String>) -> Unit
) {
    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var type by remember { mutableStateOf(initialEvent?.type ?: "") }
    var dateParams by remember { mutableStateOf(initialEvent?.date ?: System.currentTimeMillis()) }
    var positiveHours by remember { mutableStateOf(initialEvent?.positiveHours?.toString() ?: "0.0") }
    var negativeHours by remember { mutableStateOf(initialEvent?.negativeHours?.toString() ?: "0.0") }
    var mandatory by remember { mutableStateOf(initialEvent?.mandatory ?: false) }
    
    val selectedTargetWings = remember { mutableStateListOf<String>().apply { 
        addAll(initialEvent?.targetWings ?: emptyList()) 
    } }
    val selectedMandatoryWings = remember { mutableStateListOf<String>().apply { 
        addAll(initialEvent?.mandatoryWings ?: emptyList()) 
    } }
    
    // Time State
    val calendar = Calendar.getInstance()
    
    // Initialize time pickers with event times if available, otherwise current/next hour
    var startHour by remember { mutableIntStateOf(
        if (initialEvent != null && initialEvent.startTime > 0) {
            calendar.timeInMillis = initialEvent.startTime
            calendar.get(Calendar.HOUR_OF_DAY)
        } else {
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        }
    ) }
    var startMinute by remember { mutableIntStateOf(
        if (initialEvent != null && initialEvent.startTime > 0) {
            calendar.timeInMillis = initialEvent.startTime
            calendar.get(Calendar.MINUTE)
        } else {
            Calendar.getInstance().get(Calendar.MINUTE)
        }
    ) }
    
    var endHour by remember { mutableIntStateOf(
        if (initialEvent != null && initialEvent.endTime > 0) {
            calendar.timeInMillis = initialEvent.endTime
            calendar.get(Calendar.HOUR_OF_DAY)
        } else {
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + 1
        }
    ) }
    var endMinute by remember { mutableIntStateOf(
        if (initialEvent != null && initialEvent.endTime > 0) {
            calendar.timeInMillis = initialEvent.endTime
            calendar.get(Calendar.MINUTE)
        } else {
            Calendar.getInstance().get(Calendar.MINUTE)
        }
    ) }

    // Date Picker State
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateParams)
    var showDatePicker by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    val startTimePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute -> startHour = hour; startMinute = minute },
        startHour, startMinute, false
    )

    val endTimePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute -> endHour = hour; endMinute = minute },
        endHour, endMinute, false
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateParams = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialEvent != null) "Edit Event" else "Create Event") },
        text = {
            LazyColumn {
                item {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type (e.g. Camp)") }, modifier = Modifier.fillMaxWidth())
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dateParams)),
                        onValueChange = {},
                        label = { Text("Date") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                         OutlinedButton(onClick = { startTimePickerDialog.show() }, modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                             Text("Start: ${String.format("%02d:%02d", startHour, startMinute)}")
                         }
                         OutlinedButton(onClick = { endTimePickerDialog.show() }, modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                             Text("End: ${String.format("%02d:%02d", endHour, endMinute)}")
                         }
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                         OutlinedTextField(
                            value = positiveHours, 
                            onValueChange = { positiveHours = it }, 
                            label = { Text("+ Hours") },
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        )
                         OutlinedTextField(
                            value = negativeHours, 
                            onValueChange = { negativeHours = it }, 
                            label = { Text("- Hours") },
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = mandatory, onCheckedChange = { mandatory = it })
                        Text("Mandatory")
                    }
                    
                    Text("Target Wings:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    wings.forEach { wing ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedTargetWings.contains(wing.id),
                                onCheckedChange = { checked ->
                                    if (checked) selectedTargetWings.add(wing.id) else selectedTargetWings.remove(wing.id)
                                }
                            )
                            Text(wing.name)
                        }
                    }
                    
                    if (mandatory) {
                        Text("Mandatory For (Default: All Targeted):", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                        // Only show options for wings that are targeted
                        val options = wings.filter { selectedTargetWings.contains(it.id) }
                        if (options.isEmpty()) {
                            Text("Select Target Wings first", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        options.forEach { wing ->
                             Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedMandatoryWings.contains(wing.id),
                                    onCheckedChange = { checked ->
                                        if (checked) selectedMandatoryWings.add(wing.id) else selectedMandatoryWings.remove(wing.id)
                                    }
                                )
                                Text(wing.name)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (title.isNotBlank()) {
                         // Calculate timestamps
                         val startCal = Calendar.getInstance().apply { timeInMillis = dateParams; set(Calendar.HOUR_OF_DAY, startHour); set(Calendar.MINUTE, startMinute) }
                         val endCal = Calendar.getInstance().apply { timeInMillis = dateParams; set(Calendar.HOUR_OF_DAY, endHour); set(Calendar.MINUTE, endMinute) }
                         
                         onConfirm(
                             title, 
                             type, 
                             dateParams,
                             startCal.timeInMillis,
                             endCal.timeInMillis,
                             positiveHours.toDoubleOrNull() ?: 0.0,
                             negativeHours.toDoubleOrNull() ?: 0.0,
                             mandatory, 
                             selectedTargetWings.toList(),
                             if (mandatory) selectedMandatoryWings.toList() else emptyList()
                         )
                    }
                }
            ) {
                Text(if (initialEvent != null) "Update" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
