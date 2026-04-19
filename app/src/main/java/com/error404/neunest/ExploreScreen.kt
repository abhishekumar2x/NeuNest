package com.error404.neunest

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExploreScreen(
    exploreViewModel: ExploreViewModel,
    onSelectModel: (name: String, modelPath: String) -> Unit
) {
    val context = LocalContext.current
    val uiState by exploreViewModel.uiState.collectAsState()

    val stats by rememberSystemStats()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { exploreViewModel.onUriSelected(context, it) }
    }

    LaunchedEffect(Unit) {
        exploreViewModel.refreshFile(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("NeuNest")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding
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
            item {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(24.dp),
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Run models locally",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Import models from your device to get started.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        FilledTonalButton(
                            onClick = {
                                launcher.launch("*/*")
                            },
                            enabled = !uiState.isCopying
                        ) {
                            if (uiState.isCopying) {
                                CircularProgressIndicator(
                                    progress = { uiState.copyProgress },
                                    modifier = Modifier.size(30.dp)
                                )
                            } else {
                                Text("Import model")
                            }
                        }
                    }
                }
            }

            if (uiState.models.isNotEmpty()) {
                stickyHeader {
                    Text(
                        text = "Your models",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                itemsIndexed(uiState.models) { index, file ->
                    val model = matcher.find { it.id == file.name }

                    if (model != null) {
                        val shape = when (index) {
                            0 -> RoundedCornerShape(20.dp, 20.dp, 8.dp, 8.dp)
                            uiState.models.lastIndex -> RoundedCornerShape(8.dp, 8.dp, 20.dp, 20.dp)
                            else -> RoundedCornerShape(8.dp)
                        }

                        val icon = if (model.vendor == Vendor.GEMMA) R.drawable.ic_gemma
                        else if (model.vendor == Vendor.QWEN) R.drawable.ic_qwen
                        else R.drawable.ic_deepseek

                        ListItem(
                            leadingContent = {
                                Image(
                                    painterResource(icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp)
                                )
                            },
                            headlineContent = { Text(model.name) },
                            supportingContent = {
                                Column {
                                    Text(file.length().formatSize())
                                    Text(model.types.joinToString(" • "))
                                    SuggestionChip(
                                        onClick = { },
                                        label = {
                                            Text("${model.minDeviceMemoryInGb}GB RAM required")
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .padding(16.dp, 2.dp)
                                .clip(shape)
                                .combinedClickable(
                                    onClick = {
                                        onSelectModel(model.name, file.path)
                                    },
                                    onLongClick = {
                                        if (file.exists()) {
                                            file.delete()
                                            exploreViewModel.refreshFile(context)
                                        }
                                    }
                                ),
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Chip(
    icon: Int,
    text: String
) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onBackground.copy(0.22f),
                shape = CircleShape
            )
            .padding(16.dp, 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null
            )
            Text(text, fontSize = 14.sp)
        }
    }
}

fun Long.formatSize(): String {
    return when {
        this > 1024 * 1024 -> "${this / (1024 * 1024)} MB"
        this > 1024 -> "${this / 1024} KB"
        else -> "$this B"
    }
}