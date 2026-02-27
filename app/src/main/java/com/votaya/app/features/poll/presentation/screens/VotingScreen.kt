package com.votaya.app.features.poll.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.votaya.app.features.poll.presentation.components.RoomCodeCard
import com.votaya.app.features.poll.presentation.components.VoteOptionCard
import com.votaya.app.features.poll.presentation.viewmodels.PollViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotingScreen(
    roomCode: String,
    onPollClosed: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: PollViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedOption by rememberSaveable { mutableIntStateOf(-1) }
    val snackbarHostState = remember { SnackbarHostState() }

    val room = uiState.room
    val isCreator = room?.isCreator ?: false
    val hasVoted = uiState.hasVoted
    val totalVotes = room?.options?.sumOf { it.votes } ?: 0
    
    LaunchedEffect(roomCode) {
        viewModel.connectAndJoinRoom(roomCode)
    }

    LaunchedEffect(uiState.isPollClosed) {
        if (uiState.isPollClosed) {
            onPollClosed(roomCode)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Votación") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (room == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Room code card
                    RoomCodeCard(
                        roomCode = room.code,
                        voterCount = room.totalVoters
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Question
                    Text(
                        text = room.question,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Options
                itemsIndexed(room.options) { index, option ->
                    VoteOptionCard(
                        option = option,
                        index = index,
                        totalVotes = totalVotes,
                        isSelected = selectedOption == index,
                        enabled = !hasVoted && !isCreator,
                        showResults = isCreator || hasVoted,
                        onClick = {
                            if (!hasVoted && !isCreator) {
                                selectedOption = index
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isCreator && !hasVoted) {
                        // Vote button
                        Button(
                            onClick = {
                                if (selectedOption >= 0) {
                                    viewModel.vote(roomCode, selectedOption)
                                }
                            },
                            enabled = selectedOption >= 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Votar", fontSize = 16.sp)
                        }
                    }

                    if (!isCreator && hasVoted) {
                        Text(
                            text = "✓ Ya votaste. Esperando resultados...",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (isCreator) {
                        // Close poll button
                        Button(
                            onClick = { viewModel.closePoll(roomCode) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Cerrar votación", fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Como creador, puedes ver los votos en tiempo real",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
