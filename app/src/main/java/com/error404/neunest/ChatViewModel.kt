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
    val pdfLoaded: Boolean = false,
)

class ChatViewModel(
    private val inference: Inference,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()
    private var pdfChunks: List<Chunk> = emptyList()

    // ── PDF ────────────────────────────────────────────────────────────────────

    fun loadPdf(filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pages = PDFDecoder().extractAll(filePath).take(20)
                pdfChunks = Chunker(maxChars = 800, overlap = 120).chunk(pages)
                _uiState.update { it.copy(pdfLoaded = pdfChunks.isNotEmpty()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(currentResponse = "PDF error: ${e.message}") }
            }
        }
    }

    // ── Init / lifecycle ───────────────────────────────────────────────────────

    init { observeInference() }

    private fun observeInference() {
        viewModelScope.launch {
            inference.state.collect { state ->
                _uiState.update { it.copy(status = state) }
            }
        }
    }

    fun loadModel(modelPath: String) {
        viewModelScope.launch { inference.loadModel(modelPath) }
    }

    // ── Send message ───────────────────────────────────────────────────────────

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
                val finalPrompt = buildPrompt(message)

                inference.stream(finalPrompt).collect { token ->
                    _uiState.update { it.copy(currentResponse = it.currentResponse + token) }
                }

                // Use Message.system() for assistant role — this is what LiteRT LLM SDK uses.
                // Message.model() does NOT exist in com.google.ai.edge.litertlm.
                _uiState.update {
                    it.copy(
                        messages = it.messages + Message.system(it.currentResponse),
                        currentResponse = "",
                        isStreaming = false
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(currentResponse = "Error: ${e.message}", isStreaming = false)
                }
            }
        }
    }

    private fun buildPrompt(question: String): String {
        if (pdfChunks.isEmpty()) return question

        val hits = KeywordRetriever().search(question, pdfChunks, k = 3)

        // Trim at chunk boundary — never mid-sentence
        val contextParts = mutableListOf<String>()
        var total = 0
        for (hit in hits) {
            val part = "[p${hit.pageStart}] ${hit.text}"
            if (total + part.length > 1800) break
            contextParts.add(part)
            total += part.length
        }

        val context = contextParts.joinToString("\n\n")

        // ── IMPORTANT: do NOT tell the model to say "Not found in document"
        // when you can't guarantee the retriever found the right chunks.
        // Instead, just give it the context and let it answer naturally.
        // ──────────────────────────────────────────────────────────────────
        return if (context.isNotBlank()) {
            """
You are a helpful assistant. Use the context excerpts below to answer the question.
Answer as directly as possible. If the context does not contain enough information, answer from your own knowledge and say so briefly.

Context:
$context

Question: $question
            """.trimIndent()
        } else {
            question
        }
    }

    fun resetChat() {
        inference.resetConversation()
        pdfChunks = emptyList()
        _uiState.update {
            it.copy(messages = emptyList(), currentResponse = "", pdfLoaded = false)
        }
    }

    fun onDestroy() {
        viewModelScope.launch { inference.close() }
    }
}