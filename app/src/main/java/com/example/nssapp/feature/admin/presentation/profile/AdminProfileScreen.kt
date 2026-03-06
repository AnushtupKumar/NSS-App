package com.example.nssapp.feature.admin.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nssapp.feature.admin.presentation.events.EventItem

@Composable
fun AdminProfileScreen(
    onLogout: () -> Unit,
    viewModel: AdminProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val passwordChangeState by viewModel.passwordChangeState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var showChangePasswordDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var newPassword by remember { androidx.compose.runtime.mutableStateOf("") }

    LaunchedEffect(passwordChangeState) {
        if (passwordChangeState is PasswordChangeState.Success) {
            android.widget.Toast.makeText(context, "Password changed successfully", android.widget.Toast.LENGTH_SHORT).show()
            showChangePasswordDialog = false
            viewModel.clearPasswordChangeState()
            newPassword = ""
        }
    }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Admin Profile", style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = { viewModel.logout(onLogout) }) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.padding(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        val email = when (val state = uiState) {
                            is AdminProfileUiState.Success -> state.email
                            else -> "Loading..."
                        }
                        Text(
                            text = "Admin Email",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = email,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showChangePasswordDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Password")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("My Events", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (val state = uiState) {
                    is AdminProfileUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is AdminProfileUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    is AdminProfileUiState.Success -> {
                        if (state.events.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No events created yet.")
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(state.events) { event ->
                                    EventItem(event = event, onClick = {})
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showChangePasswordDialog) {
            AlertDialog(
                onDismissRequest = {
                    showChangePasswordDialog = false
                    viewModel.clearPasswordChangeState()
                    newPassword = ""
                },
                title = { Text("Change Password") },
                text = {
                    Column {
                        Text("Enter your new password.")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New Password") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        when (passwordChangeState) {
                            is PasswordChangeState.Loading -> {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                            }

                            is PasswordChangeState.Error -> {
                                Text(
                                    (passwordChangeState as PasswordChangeState.Error).message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            else -> {}
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.changePassword(newPassword) },
                        enabled = newPassword.isNotBlank() && passwordChangeState !is PasswordChangeState.Loading
                    ) {
                        Text("Change")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showChangePasswordDialog = false
                        viewModel.clearPasswordChangeState()
                        newPassword = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
