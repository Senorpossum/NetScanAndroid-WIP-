package com.example.networkscanner.ui.state

import androidx.compose.runtime.Stable

@Stable
sealed class ScanUiState<out T> {
    object Idle : ScanUiState<Nothing>()
    data class Scanning(val progress: Float? = null) : ScanUiState<Nothing>()
    data class Success<T>(val data: T) : ScanUiState<T>()
    data class Error(val message: String) : ScanUiState<Nothing>()
}
