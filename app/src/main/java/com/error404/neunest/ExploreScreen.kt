package com.error404.neunest

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExploreScreen(
    exploreViewModel: ExploreViewModel,
    onSelectModel: (modelPath: String) -> Unit
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

    Scaffold { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            item {
                Text("Battery voltage: ${stats.voltage}")
                Text("Battery temperature: ${stats.temperature}")
                Text("Memory: ${stats.memory.first} / ${stats.memory.second}")
            }
            item {
                FilledTonalButton(
                    onClick = {
                        launcher.launch("*/*")
                    },
                    enabled = !uiState.isCopying
                ) {
                    Text("Import model")
                }
            }

            item {
                if (uiState.isCopying) {
                    Text("${(uiState.copyProgress * 100).toInt()}%")
                    LinearWavyProgressIndicator(progress = { uiState.copyProgress })
                }
            }
            items(uiState.models) { file ->
                if (file.extension == "litertlm") {
                    Column(
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                onSelectModel(file.path)
                            },
                            onLongClick = {
                                if (file.exists()) {
                                    file.delete()
                                    exploreViewModel.refreshFile(context)
                                }
                            }
                        )
                    ) {
                        Text(file.name)
                        Text(file.length().formatSize())
                        HorizontalDivider()
                    }
                }
            }
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