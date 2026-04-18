package com.error404.neunest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.error404.neunest.Inference.State
import com.google.ai.edge.litertlm.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val status: State = State.Idle,
    val messages: List<Message> = emptyList(),
    val currentResponse: String = "",
    val isStreaming: Boolean = false,
)

class ChatViewModel(
    private val inference: Inference,
): ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeInference()
    }

    private fun observeInference() {
        viewModelScope.launch {
            inference.state.collect { state ->
                _uiState.update {
                    it.copy(status = state)
                }
            }
        }
    }

    fun loadModel(modelPath: String) {
        viewModelScope.launch {
            inference.loadModel(modelPath)
        }
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    messages = it.messages + Message.user(message),
                    currentResponse = "",
                    isStreaming = true
                )
            }

            try {
                inference.stream(message).collect { chunk ->
                    _uiState.update {
                        it.copy(
                            currentResponse = it.currentResponse + chunk
                        )
                    }
                }

                _uiState.update {
                    it.copy(
                        messages = it.messages + Message.system(it.currentResponse),
                        currentResponse = "",
                        isStreaming = false
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        currentResponse = "Error: ${e.message}",
                        isStreaming = false
                    )
                }
            }
        }
    }

    fun resetChat() {
        inference.resetConversation()

        _uiState.update {
            it.copy(
                messages = emptyList(),
                currentResponse = ""
            )
        }
    }

    fun onDestroy() {
        viewModelScope.launch {
            inference.close()
        }
    }
}