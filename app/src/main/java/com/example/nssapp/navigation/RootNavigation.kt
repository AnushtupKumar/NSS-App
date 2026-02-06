package com.example.nssapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nssapp.feature.admin.presentation.AdminHomeScreen
import com.example.nssapp.feature.auth.presentation.AuthState
import com.example.nssapp.feature.auth.presentation.AuthViewModel
import com.example.nssapp.feature.auth.presentation.LoginScreen
import com.example.nssapp.feature.student.presentation.StudentHomeScreen
import com.example.nssapp.feature.student.presentation.profile.StudentProfileScreen
import androidx.navigation.compose.rememberNavController

@Composable
fun RootNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.SuccessAdmin -> {
                navController.navigate("admin_home") {
                    popUpTo("login") { inclusive = true }
                }
            }
            is AuthState.SuccessStudent -> {
                navController.navigate("student_home") {
                    popUpTo("login") { inclusive = true }
                }
            }
            else -> Unit
        }
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(viewModel = authViewModel)
        }
        composable("admin_home") {
            AdminHomeScreen()
        }
        composable("student_home") {
            StudentHomeScreen(
                onProfileClick = { navController.navigate("student_profile") }
            )
        }
        composable("student_profile") {
            StudentProfileScreen(
                onBackClick = { navController.popBackStack() },
                onLogout = { 
                     navController.navigate("login") {
                         popUpTo(0) { inclusive = true }
                     }
                }
            )
        }
    }
}
