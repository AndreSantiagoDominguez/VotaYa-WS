package com.votaya.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.votaya.app.features.auth.presentation.screens.LoginScreen
import com.votaya.app.features.auth.presentation.screens.RegisterScreen
import com.votaya.app.features.auth.presentation.viewmodels.AuthViewModel
import com.votaya.app.features.poll.presentation.screens.CreatePollScreen
import com.votaya.app.features.poll.presentation.screens.HomeScreen
import com.votaya.app.features.poll.presentation.screens.JoinRoomScreen
import com.votaya.app.features.poll.presentation.screens.VotingScreen
import com.votaya.app.features.poll.presentation.screens.ResultsScreen

@Composable
fun VotaYaNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()
    
    // ViewModel compartido para todo el flujo de votación
    // Al estar fuera del NavHost, sobrevive a las transiciones de pantalla
    val pollViewModel: com.votaya.app.features.poll.presentation.viewmodels.PollViewModel = hiltViewModel()

    val startDestination = if (authState.token != null) Routes.Home.route else Routes.Login.route

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Routes.Register.route) },
                onLoginSuccess = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Home.route) {
            HomeScreen(
                viewModel = pollViewModel,
                onCreatePoll = { navController.navigate(Routes.CreatePoll.route) },
                onJoinRoom = { navController.navigate(Routes.JoinRoom.route) },
                onLogout = {
                    authViewModel.logout()
                    pollViewModel.clearError() // Opcional: limpiar estado al salir
                    navController.navigate(Routes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CreatePoll.route) {
            CreatePollScreen(
                viewModel = pollViewModel,
                onPollCreated = { roomCode ->
                    navController.navigate(Routes.Voting.createRoute(roomCode)) {
                        popUpTo(Routes.Home.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.JoinRoom.route) {
            JoinRoomScreen(
                viewModel = pollViewModel,
                onRoomJoined = { roomCode ->
                    navController.navigate(Routes.Voting.createRoute(roomCode)) {
                        popUpTo(Routes.Home.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.Voting.route,
            arguments = listOf(navArgument("roomCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
            VotingScreen(
                roomCode = roomCode,
                viewModel = pollViewModel,
                onPollClosed = { code ->
                    navController.navigate(Routes.Results.createRoute(code)) {
                        popUpTo(Routes.Home.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.Results.route,
            arguments = listOf(navArgument("roomCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
            ResultsScreen(
                roomCode = roomCode,
                onBackToHome = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
