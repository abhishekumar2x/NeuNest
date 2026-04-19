package com.error404.neunest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.error404.neunest.Inference.State
import com.google.ai.edge.litertlm.Message
import kotlinx.coroutines.Dispatchers
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
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()
    private var pdfChunks: List<Chunk> = emptyList()

    fun loadPdf(filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val extractor = PDFDecoder()
                val pages = extractor.extractAll(filePath).take(15)

                val chunker = Chunker(maxChars = 400, overlap = 80)
                pdfChunks = chunker.chunk(pages)

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(currentResponse = "PDF error: ${e.message}")
                }
            }
        }
    }

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

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    messages = it.messages + Message.user(message),
                    currentResponse = "",
                    isStreaming = true
                )
            }

            try {
                val retriever = KeywordRetriever()

                val hits = if (pdfChunks.isNotEmpty()) {
                    retriever.search(message, pdfChunks, k = 3)
                } else emptyList()

                val context = hits.joinToString("\n\n") {
                    "[p${it.pageStart}] ${it.text}"
                }

                val trimmedContext = context.take(1500)

                val finalPrompt = if (trimmedContext.isNotBlank()) {
                    """
    Use the context to answer the question.
    If not found, say "Not in document".

    Context:
    $trimmedContext

    Question:
    $message
    """.trimIndent()
                } else {
                    message
                }
                inference.stream(finalPrompt).collect { chunk ->
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