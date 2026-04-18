package com.error404.neunest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.Role

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatScreen(
    modelPath: String,
    chatViewModel: ChatViewModel
) {
    val uiState by chatViewModel.uiState.collectAsState()
    var input by remember { mutableStateOf("") }
    val stats by rememberSystemStats()

    LaunchedEffect(modelPath) {
        chatViewModel.loadModel(modelPath)
    }
    DisposableEffect(Unit) {
        onDispose {
            chatViewModel.onDestroy()
        }
    }

    when (uiState.status) {
        is Inference.State.Error -> {
            Text("error")
        }

        Inference.State.Idle -> {
            Text("error")
        }

        Inference.State.Loading -> {
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

        Inference.State.Ready -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    reverseLayout = true
                ) {
                    item {
                        FlowRow(
                            modifier = Modifier.padding(16.dp, 0.dp)
                        ) {
                            Chip(R.drawable.ic_voltage, "${stats.voltage}V")
                            Chip(R.drawable.ic_thermostats, "${stats.temperature}°C")
                            Chip(R.drawable.ic_memory, "${stats.memory.first} MB / ${stats.memory.second} MB")
                        }
                    }
                    val allMessages = buildList {
                        addAll(uiState.messages)
                        if (uiState.isStreaming && uiState.currentResponse.isNotEmpty()) {
                            add(Message.system(uiState.currentResponse))
                        }
                    }

                    items(allMessages.reversed()) { message ->
                        ChatBubble(message = message)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            chatViewModel.sendMessage(input)
                            input = ""
                        },
                        enabled = !uiState.isStreaming
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message) {
    val isUser = message.role == Role.USER

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
                .widthIn(max = 300.dp)
        ) {
            Text(text = message.toString(), color = MaterialTheme.colorScheme.background)
        }
    }
}