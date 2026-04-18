package com.error404.neunest

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Every stored message string is prefixed with a role tag so we can
 * distinguish user messages from assistant messages without changing
 * ChatUiState or ChatViewModel.
 *
 * Existing code in ChatViewModel adds raw user text and raw assistant text to
 * the same list, interleaved.  We reconstruct the intent here:
 *   - Odd-indexed messages (0, 2, 4 …) are from the user.
 *   - Even-indexed messages (1, 3, 5 …) are from the assistant.
 */
private fun isUserMessage(index: Int) = index % 2 == 0

// ---------------------------------------------------------------------------
// Screens
// ---------------------------------------------------------------------------

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
        onDispose { chatViewModel.onDestroy() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        when (val state = uiState.status) {
            is Inference.State.Loading -> LoadingView()
            is Inference.State.Error -> ErrorView(state.message)
            is Inference.State.Ready -> ChatContent(
                uiState = uiState,
                onSend = chatViewModel::sendMessage,
                onReset = chatViewModel::resetChat
            )

            else -> LoadingView()
        }
    }
}

// ---------------------------------------------------------------------------
// Loading
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularWavyProgressIndicator(
                modifier = Modifier.size(56.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Loading model…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Error
// ---------------------------------------------------------------------------

@Composable
fun ErrorView(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Something went wrong",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Main chat content
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatContent(
    uiState: ChatUiState,
    onSend: (String) -> Unit,
    onReset: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll to bottom whenever messages or the live response changes.
    val totalItems = uiState.messages.size + if (uiState.currentResponse.isNotEmpty()) 1 else 0
    LaunchedEffect(totalItems, uiState.currentResponse) {
        if (totalItems > 0) {
            scope.launch {
                listState.animateScrollToItem(totalItems - 1)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ChatTopBar(onReset = onReset)
        },
        bottomBar = {
            ChatInputBar(
                value = input,
                onValueChange = { input = it },
                isStreaming = uiState.isStreaming,
                onSend = {
                    if (input.isNotBlank()) {
                        onSend(input.trim())
                        input = ""
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.messages.isEmpty() && uiState.currentResponse.isEmpty()) {
            EmptyState(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp)
                    .imeNestedScroll(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                itemsIndexed(uiState.messages) { index, message ->
                    val isUser = isUserMessage(index)
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
                    ) {
                        ChatBubble(text = message, isUser = isUser)
                    }
                }

                // Live streaming response
                if (uiState.currentResponse.isNotEmpty()) {
                    item {
                        ChatBubble(
                            text = uiState.currentResponse,
                            isUser = false,
                            isLive = true
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Top bar
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(onReset: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "NeuNest",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "On-device AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            IconButton(onClick = onReset) {
                Icon(
                    painter = painterResource(R.drawable.ic_gemma),
                    contentDescription = "Reset conversation",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

// ---------------------------------------------------------------------------
// Bubble
// ---------------------------------------------------------------------------

@Composable
fun ChatBubble(
    text: String,
    isUser: Boolean,
    isLive: Boolean = false
) {
    val bubbleColor = if (isUser)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant

    val textColor = if (isUser)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    val shape = if (isUser) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        // Avatar for assistant
        if (!isUser) {
            AssistantAvatar()
            Spacer(Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(shape)
                    .background(bubbleColor)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = text,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                    // Blinking cursor while streaming
                    if (isLive) {
                        Spacer(Modifier.width(2.dp))
                        BlinkingCursor(color = textColor)
                    }
                }
            }
        }

        // Spacer for user messages (keep avatar column width consistent)
        if (isUser) {
            Spacer(Modifier.width(8.dp + 28.dp)) // mirror avatar width
        }
    }
}

// ---------------------------------------------------------------------------
// Assistant avatar
// ---------------------------------------------------------------------------

@Composable
fun AssistantAvatar() {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "N",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

// ---------------------------------------------------------------------------
// Blinking cursor
// ---------------------------------------------------------------------------

@Composable
fun BlinkingCursor(color: Color) {
    val alpha by rememberInfiniteTransition(label = "cursor").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(14.dp)
            .background(color.copy(alpha = alpha))
    )
}

// ---------------------------------------------------------------------------
// Input bar
// ---------------------------------------------------------------------------

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    isStreaming: Boolean,
    onSend: () -> Unit
) {
    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Message…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                maxLines = 5,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )

            val canSend = value.isNotBlank() && !isStreaming
            FilledIconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            ) {
                if (isStreaming) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_gemma),
                        contentDescription = "Send",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Empty state
// ---------------------------------------------------------------------------

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "N",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Start a conversation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Ask anything — your model runs entirely on device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}