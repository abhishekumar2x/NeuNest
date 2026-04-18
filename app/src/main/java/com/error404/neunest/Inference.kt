package com.error404.neunest

import android.content.Context
import com.google.ai.edge.litertlm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Inference(
    private val context: Context
) {
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    private val mutex = Mutex()

    private val _state = MutableStateFlow<State>(State.Idle)
    val state = _state.asStateFlow()

    private var currentModel: String? = null

    sealed class State {
        object Idle : State()
        object Loading : State()
        object Ready : State()
        data class Error(val message: String) : State()
    }

    suspend fun loadModel(modelPath: String) {
        mutex.withLock {
            if (modelPath == currentModel && engine != null) return

            _state.update {
                State.Loading
            }

            try {
                closeInternal()

                withContext(Dispatchers.IO) {
                    val engineConfig = EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.GPU(),
                        cacheDir = context.cacheDir.path
                    )

                    val newEngine = Engine(engineConfig)
                    newEngine.initialize()

                    engine = newEngine
                    currentModel = modelPath
                }

                _state.update {
                    State.Ready
                }
            } catch (e: Exception) {
                _state.update {
                    State.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    private fun ensureConversation() {
        val eng = engine ?: throw IllegalStateException("Engine not initialized")

        if (conversation == null) {
            val config = ConversationConfig(
                systemInstruction = Contents.of("You are a helpful assistant. Make sure your responses are small and to the point.")
            )
            conversation = eng.createConversation(config)
        }
    }

    fun resetConversation() {
        conversation?.close()
        conversation = null
    }

    fun stream(message: String): Flow<String> = flow {
        if (engine == null) {
            throw IllegalStateException("Model not loaded")
        }

        ensureConversation()

        conversation!!
            .sendMessageAsync(message)
            .collect { chunk ->
                emit(chunk.toString())
            }
    }.flowOn(Dispatchers.IO)

    suspend fun close() {
        mutex.withLock {
            closeInternal()
            currentModel = null
            _state.update {
                State.Idle
            }
        }
    }

    private fun closeInternal() {
        try {
            conversation?.close()
        } catch (_: Exception) {}

        try {
            engine?.close()
        } catch (_: Exception) {}

        conversation = null
        engine = null
    }
}