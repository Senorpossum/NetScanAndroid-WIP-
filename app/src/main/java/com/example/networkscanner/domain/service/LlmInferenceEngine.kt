package com.example.networkscanner.domain.service

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmInferenceEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var llmInference: LlmInference? = null
    private val initializationMutex = Mutex()

    /**
     * Safely initializes the LLM logic unit.
     * Prevents native crashes by wrapping in a robust try-catch-throwable.
     */
    suspend fun initialize(modelPath: String): Boolean = initializationMutex.withLock {
        if (llmInference != null) return true
        
        return withContext(Dispatchers.IO) {
            try {
                // Ensure model file is readable and not zero-byte
                val file = java.io.File(modelPath)
                if (!file.exists() || file.length() < 1000000) return@withContext false

                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(1024)
                    .build()
                llmInference = LlmInference.createFromOptions(context, options)
                android.util.Log.i("LlmEngine", "Intelligence unit synchronized.")
                true
            } catch (e: Throwable) {
                android.util.Log.e("LlmEngine", "Logical initialization failure: ${e.message}")
                false
            }
        }
    }

    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.Default) {
        val inference = llmInference ?: return@withContext "Engine offline"
        return@withContext try {
            inference.generateResponse(prompt)
        } catch (e: Exception) {
            "Response Interrupted: ${e.message}"
        }
    }

    fun generateResponseAsync(prompt: String): Flow<String> = callbackFlow {
        val inference = llmInference
        if (inference == null) {
            trySend("Engine offline")
            close()
            return@callbackFlow
        }

        try {
            // Bridge synchronous generateResponse to Flow
            val result = withContext(Dispatchers.Default) {
                inference.generateResponse(prompt)
            }
            trySend(result)
        } catch (e: Exception) {
            trySend("Inference Error: ${e.message}")
        }
        close()
        awaitClose {}
    }

    fun isInitialized(): Boolean = llmInference != null
}
