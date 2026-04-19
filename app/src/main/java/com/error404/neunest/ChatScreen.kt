package com.error404.neunest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.sp
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.Role
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import java.io.File

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatScreen(
    name: String,
    modelPath: String,
    chatViewModel: ChatViewModel
) {
    val uiState by chatViewModel.uiState.collectAsState()
    var input by remember { mutableStateOf("") }
    val stats by rememberSystemStats()

    val context = LocalContext.current

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val path = uriToFile(context, it)
            chatViewModel.loadPdf(path)
        }
    }

    LaunchedEffect(modelPath) {
        chatViewModel.loadModel(modelPath)
    }

    DisposableEffect(Unit) {
        onDispose { chatViewModel.onDestroy() }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(name) })
        }
    ) { padding ->

        when (uiState.status) {

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
                        .padding(padding)
                ) {

                    // FIX #7: use Message.model() for the live streaming bubble,
                    // matching the role used when the message is committed
                    val messages = buildList {
                        addAll(uiState.messages)
                        if (uiState.isStreaming && uiState.currentResponse.isNotEmpty()) {
                            add(Message.system(uiState.currentResponse))
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        reverseLayout = true,
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(messages.reversed()) { msg ->
                            ChatBubble(msg)
                        }
                    }

                    InputBar(
                        input = input,
                        onInputChange = { input = it },
                        onSend = {
                            chatViewModel.sendMessage(input)
                            input = ""
                        },
                        onPickPdf = {
                            pdfPicker.launch("application/pdf")
                        },
                        isStreaming = uiState.isStreaming,
                        pdfLoaded = uiState.pdfLoaded,   // NEW: show PDF badge
                        stats = stats
                    )
                }
            }

            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Something went wrong")
                }
            }
        }
    }
}

@Composable
private fun InputBar(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isStreaming: Boolean,
    pdfLoaded: Boolean,          // NEW
    stats: SystemStats,
    onPickPdf: () -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {

            AnimatedVisibility(isStreaming) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LoadingIndicator(Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Thinking...")
                }
            }

            Text(
                text = "${stats.temperature}°C • ${stats.memory.first} MB / ${stats.memory.second} MB",
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                TextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message...") },
                    maxLines = 4,
                    shape = RoundedCornerShape(20.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(Modifier.width(8.dp))

                // NEW: button label shows whether a PDF is already loaded
                Button(onClick = onPickPdf) {
                    Text(if (pdfLoaded) "PDF ✓" else "PDF")
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = onSend,
                    enabled = input.isNotBlank() && !isStreaming
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message) {
    // FIX #1 (display side): model role renders on the left like system did,
    // but isUser stays correct because we only check Role.USER
    val isUser = message.role == Role.USER

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (isUser)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = message.toString(),
                color = if (isUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun uriToFile(context: Context, uri: Uri): String {
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw IllegalStateException("Cannot open PDF")

    val file = File.createTempFile("picked_pdf", ".pdf", context.cacheDir)

    file.outputStream().use { output ->
        inputStream.copyTo(output)
    }

    return file.absolutePath
}