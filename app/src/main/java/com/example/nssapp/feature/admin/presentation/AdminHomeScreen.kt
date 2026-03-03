package com.example.nssapp.feature.admin.presentation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
    object Students : AdminScreen("admin_students", "Students", Icons.Default.Group)
    object Wings : AdminScreen("admin_wings", "Wings", Icons.Default.Edit)
    object Profile : AdminScreen("admin_profile", "Profile", Icons.Default.AccountCircle)
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
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                screen.icon, 
                                contentDescription = screen.title,
                                modifier = if (selected) Modifier.padding(bottom = 4.dp) else Modifier
                            ) 
                        },
                        label = { Text(screen.title, style = MaterialTheme.typography.labelMedium) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AdminScreen.Events.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f) },
            exitTransition = { fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 1.05f) }
        ) {
            composable(AdminScreen.Events.route) { EventListScreen(onEventClick = onEventClick) }
            composable(AdminScreen.Students.route) { StudentListScreen() }
            composable(AdminScreen.Wings.route) { WingManagementScreen() }
            composable(AdminScreen.Profile.route) { AdminProfileScreen(onLogout = onLogout) }
        }
    }
}
