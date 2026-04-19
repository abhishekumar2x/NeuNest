package com.error404.neunest

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import org.koin.compose.viewmodel.koinViewModel

sealed interface Screen {
    data object Explore

    data class Chat(val name: String, val modelPath: String)
}

@Composable
fun Navigation() {
    val backStack = remember { mutableStateListOf<Any>(Screen.Explore) }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key ->
            when (key) {
                is Screen.Explore -> NavEntry(key) {
                    val exploreViewModel = koinViewModel<ExploreViewModel>()
                    ExploreScreen(exploreViewModel) { name, modelPath ->
                        backStack.add(Screen.Chat(name, modelPath))
                    }
                }

                is Screen.Chat -> NavEntry(key) {
                    val chatViewModel = koinViewModel<ChatViewModel>()
                    ChatScreen(key.name, key.modelPath, chatViewModel)
                }

                else -> NavEntry(key) {
                    Text("Unknown Screen")
                }
            }
        }
    )
}