package com.example.networkscanner.domain.service

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    data class Completed(val fileUri: Uri) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    
    // Default public link for Gemma 2B (as a fallback)
    private val modelUrl = "https://huggingface.co/google/gemma-2-2b-it-tflite/resolve/main/gemma-2-2b-it-cpu-int4.bin?download=true"
    private val modelFileName = "gemma2_instruct.bin"
    private val modelFile = File(context.filesDir, modelFileName)

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun isModelDownloaded(): Boolean = modelFile.exists() && modelFile.length() > 500000000

    fun getModelPath(): String = modelFile.absolutePath

    suspend fun downloadModel() = withContext(Dispatchers.IO) {
        if (isModelDownloaded()) {
            _downloadState.value = DownloadState.Completed(Uri.fromFile(modelFile))
            return@withContext
        }

        val request = DownloadManager.Request(Uri.parse(modelUrl))
            .setTitle("NetSentinel AI Unit")
            .setDescription("Synchronizing Gemma Intelligence...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(modelFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        try {
            if (modelFile.exists()) modelFile.delete()
            val downloadId = downloadManager.enqueue(request)
            monitorDownload(downloadId)
        } catch (e: Exception) {
            _downloadState.value = DownloadState.Error("Sync failed: ${e.message}")
        }
    }

    /**
     * Imports a local .bin or .task file selected by the user.
     * This is the primary method to bypass mirror rejections.
     */
    suspend fun importModel(uri: Uri) = withContext(Dispatchers.IO) {
        _downloadState.value = DownloadState.Downloading(0)
        try {
            if (modelFile.exists()) modelFile.delete()
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(modelFile).use { outputStream ->
                    val buffer = ByteArray(1024 * 1024) // 1MB buffer
                    var bytesRead: Int
                    var totalRead = 0L
                    val fileSize = try {
                        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
                    } catch (e: Exception) { -1L }

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (fileSize > 0) {
                            _downloadState.value = DownloadState.Downloading(((totalRead * 100) / fileSize).toInt())
                        }
                    }
                }
            }

            if (isModelDownloaded()) {
                _downloadState.value = DownloadState.Completed(Uri.fromFile(modelFile))
            } else {
                throw Exception("Imported file is too small or invalid.")
            }
        } catch (e: Exception) {
            _downloadState.value = DownloadState.Error("Import failed: ${e.message}")
        }
    }

    private suspend fun monitorDownload(downloadId: Long) {
        var isDownloading = true
        while (isDownloading) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                if (bytesTotal > 0) {
                    val progress = ((bytesDownloaded * 100) / bytesTotal).toInt()
                    _downloadState.value = DownloadState.Downloading(progress)
                }

                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        isDownloading = false
                        _downloadState.value = DownloadState.Completed(Uri.fromFile(modelFile))
                    }
                    DownloadManager.STATUS_FAILED -> {
                        isDownloading = false
                        _downloadState.value = DownloadState.Error("Sync failed: Code ${cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))}")
                    }
                }
            }
            cursor?.close()
            if (isDownloading) delay(2000)
        }
    }
}
