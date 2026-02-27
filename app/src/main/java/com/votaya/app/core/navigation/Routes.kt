package com.votaya.app.core.navigation

sealed class Routes(val route: String) {
    data object Login : Routes("login")
    data object Register : Routes("register")
    data object Home : Routes("home")
    data object CreatePoll : Routes("create_poll")
    data object JoinRoom : Routes("join_room")
    data object Voting : Routes("voting/{roomCode}") {
        fun createRoute(roomCode: String) = "voting/$roomCode"
    }
    data object Results : Routes("results/{roomCode}") {
        fun createRoute(roomCode: String) = "results/$roomCode"
    }
}
