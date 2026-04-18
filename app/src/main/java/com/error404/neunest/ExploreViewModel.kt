package com.error404.neunest

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ExploreUiState(
    val models: List<File> = emptyList(),
    val isCopying: Boolean = false,
    val copyProgress: Float = 0f,
)

class ExploreViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState = _uiState.asStateFlow()

    fun onUriSelected(context: Context, uri: Uri) {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
            ?: return

        cursor.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)

            if (nameIndex == -1 || sizeIndex == -1 || !it.moveToFirst()) return

            val fileName = it.getString(nameIndex)
            val totalBytes = it.getLong(sizeIndex)

            copy(context, uri, fileName, totalBytes)
        }
    }

    private fun copy(
        context: Context,
        uri: Uri,
        fileName: String,
        totalBytes: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isCopying = true, copyProgress = 0f) }

            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                _uiState.update { it.copy(isCopying = false) }
                return@launch
            }

            val outputFile = File(context.filesDir, fileName)
            if (outputFile.exists()) {
                _uiState.update { it.copy(isCopying = false) }
                return@launch
            }

            val buffer = ByteArray(64 * 1024)
            var bytesCopied = 0L

            try {
                inputStream.use { input ->
                    outputFile.outputStream().use { output ->
                        while (true) {
                            val bytesRead = input.read(buffer)
                            if (bytesRead == -1) break

                            output.write(buffer, 0, bytesRead)
                            bytesCopied += bytesRead

                            if (totalBytes > 0 && bytesCopied % (512 * 1024) == 0L) {
                                _uiState.update {
                                    it.copy(copyProgress = bytesCopied.toFloat() / totalBytes)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _uiState.update { it.copy(isCopying = false) }
                refreshFile(context)
            }
        }
    }

    fun refreshFile(context: Context) {
        _uiState.update {
            it.copy(models = context.filesDir.listFiles()?.toList() ?: emptyList())
        }
    }
}