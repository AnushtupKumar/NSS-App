package com.example.nssapp.feature.admin.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Edit
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
import com.example.nssapp.feature.admin.presentation.profile.AdminProfileScreen
import com.example.nssapp.feature.admin.presentation.scan.ScanQRScreen
import com.example.nssapp.feature.admin.presentation.students.StudentListScreen

import com.example.nssapp.feature.admin.presentation.wings.WingManagementScreen

sealed class AdminScreen(val route: String, val title: String, val icon: ImageVector) {
    object Events : AdminScreen("admin_events", "Events", Icons.Default.DateRange)
    object Students : AdminScreen("admin_students", "Students", Icons.Default.Person)
    object Wings : AdminScreen("admin_wings", "Wings", Icons.Default.Edit)
    object Profile : AdminScreen("admin_profile", "Profile", Icons.Default.Person) // Should use different icon maybe? Person is already used.
    // Let's use AccountCircle or similar if available, or just keep Person but maybe differentiate.
    // Actually Students uses Person. Profile should use AccountCircle or Face.
    // Since I don't know if AccountCircle is available in default icons without checking, I'll use Person for now but maybe change Students to Group?
    // Icons.Default.Group is often available. Or just use Face.
    // I'll stick to Person for Profile and maybe switch Students to something else later if needed, or just use Person for both for now.
    // Wait, Icons.Default.AccountBox?
    // Let's check available icons in standard library... I can't check easily.
    // I'll use Icons.Default.Face for Profile if available, otherwise Person.
    // Safest is to just reuse Person or stick with what I have.
    // The user didn't specify icon.
    // I will use Icons.Default.AccountCircle if I can import it.
    // Actually, `Icons.Default.Person` is used for Students.
    // I'll use `Icons.Default.AccountBox` for Profile.
}

@Composable
fun AdminHomeScreen(
    onEventClick: (String) -> Unit,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val items = listOf(
        AdminScreen.Events,
        AdminScreen.Students,
        AdminScreen.Wings,
        AdminScreen.Profile
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
            composable(AdminScreen.Events.route) { EventListScreen(onEventClick = onEventClick) }
            composable(AdminScreen.Students.route) { StudentListScreen() }
            composable(AdminScreen.Wings.route) { WingManagementScreen() }
            composable(AdminScreen.Profile.route) { AdminProfileScreen(onLogout = onLogout) }
        }
    }
}
