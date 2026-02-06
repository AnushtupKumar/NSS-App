package com.example.nssapp.feature.admin.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nssapp.feature.admin.presentation.events.EventListScreen
import com.example.nssapp.feature.admin.presentation.scan.ScanQRScreen
import com.example.nssapp.feature.admin.presentation.students.StudentListScreen

sealed class AdminScreen(val route: String, val title: String, val icon: ImageVector) {
    object Events : AdminScreen("admin_events", "Events", Icons.Default.DateRange)
    object Students : AdminScreen("admin_students", "Students", Icons.Default.Person)
    object Scan : AdminScreen("admin_scan", "Scan", Icons.Default.QrCodeScanner)
}

@Composable
fun AdminHomeScreen() {
    val navController = rememberNavController()
    val items = listOf(
        AdminScreen.Events,
        AdminScreen.Students,
        AdminScreen.Scan
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AdminScreen.Events.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AdminScreen.Events.route) { EventListScreen() }
            composable(AdminScreen.Students.route) { StudentListScreen() }
            composable(AdminScreen.Scan.route) { ScanQRScreen() }
        }
    }
}
