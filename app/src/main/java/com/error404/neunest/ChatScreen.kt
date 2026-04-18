package com.error404.neunest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChatScreen(
    modelPath: String,
    chatViewModel: ChatViewModel
) {
    val uiState by chatViewModel.uiState.collectAsState()

    LaunchedEffect(modelPath) {
        chatViewModel.loadModel(modelPath)
    }
    DisposableEffect(Unit) {
        onDispose {
            chatViewModel.onDestroy()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        when (val state = uiState.status) {
            is Inference.State.Loading -> {
                LoadingView()
            }

            is Inference.State.Error -> {
                ErrorView(state.message)
            }

            is Inference.State.Ready -> {
                ChatContent(
                    uiState = uiState,
                    onSend = chatViewModel::sendMessage,
                    onReset = chatViewModel::resetChat
                )
            }

            else -> {
                LoadingView()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularWavyProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Loading model...")
        }
    }
}

@Composable
fun ErrorView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Error: $message")
    }
}

@Composable
fun ChatContent(
    uiState: ChatUiState,
    onSend: (message: String) -> Unit,
    onReset: () -> Unit
) {
    var msg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Chat", style = MaterialTheme.typography.titleLarge)

            TextButton(onClick = onReset) {
                Text("Reset")
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            items(uiState.messages) { msg ->
                Text(
                    text = msg,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (uiState.currentResponse.isNotEmpty()) {
                item {
                    Text(
                        text = "🤖 ${uiState.currentResponse}",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            OutlinedTextField(
                value = msg,
                onValueChange = { msg = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") }
            )

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = {
                    onSend(msg)
                    msg = ""
                },
                enabled = !uiState.isStreaming
            ) {
                Text("Send")
            }
        }
    }
}