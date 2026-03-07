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
import com.example.nssapp.feature.student.presentation.scan.ScanScreen


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
            is AuthState.RequiresFaceRegistration -> {
                val studentId = (authState as AuthState.RequiresFaceRegistration).studentId
                navController.navigate("face_registration/$studentId") {
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
        composable(
            route = "face_registration/{studentId}",
            arguments = listOf(androidx.navigation.navArgument("studentId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: return@composable
            
            // Getting context and creating FaceRecognizer manually here to pass it in. 
            // In a full app this might be DI injected via Hilt.
            val context = androidx.compose.ui.platform.LocalContext.current
            val faceRecognizer = androidx.compose.runtime.remember { com.example.nssapp.util.FaceRecognizer(context) }
            val faceRegistrationViewModel: com.example.nssapp.feature.student.presentation.face.FaceRegistrationViewModel = androidx.hilt.navigation.compose.hiltViewModel()

            com.example.nssapp.feature.student.presentation.face.FaceRegistrationScreen(
                studentId = studentId,
                faceRecognizer = faceRecognizer,
                onRegistrationSuccess = {
                    navController.navigate("student_home") {
                        popUpTo("face_registration/{studentId}") { inclusive = true }
                    }
                },
                onSaveEmbedding = { embedding ->
                    faceRegistrationViewModel.saveFaceEmbedding(studentId, embedding)
                }
            )
        }
        composable("admin_home") {
            AdminHomeScreen(
                onEventClick = { eventId -> navController.navigate("event_detail/$eventId") },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("student_home") {
            StudentHomeScreen(
                onProfileClick = { navController.navigate("student_profile") },
                onScanClick = { navController.navigate("scan_screen") }
            )
        }
        composable("scan_screen") {
            ScanScreen(navController = navController)
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

        composable(
            route = "event_detail/{eventId}",
            arguments = listOf(androidx.navigation.navArgument("eventId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
            com.example.nssapp.feature.admin.presentation.events.EventDetailScreen(
                eventId = eventId,
                navController = navController
            )
        }
    }
}
