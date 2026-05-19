package com.dttrn.datfs.feature.backup.presentation

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dttrn.datfs.core.domain.common.Result
import com.dttrn.datfs.feature.backup.domain.usecase.BackupUseCase
import com.dttrn.datfs.feature.backup.domain.usecase.RestorePreview
import com.dttrn.datfs.feature.backup.domain.usecase.RestoreUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BackupUiState {
    data object Idle : BackupUiState()
    data object Loading : BackupUiState()
    data class ReadyToShare(val intent: Intent) : BackupUiState()
    data class RestorePreview(val preview: com.dttrn.datfs.feature.backup.domain.usecase.RestorePreview, val uri: Uri) : BackupUiState()
    data object Restoring : BackupUiState()
    data class RestoreSuccess(val deckCount: Int, val cardCount: Int) : BackupUiState()
    data class Error(val message: String) : BackupUiState()
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupUseCase: BackupUseCase,
    private val restoreUseCase: RestoreUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun backupJson() {
        viewModelScope.launch {
            _uiState.value = BackupUiState.Loading
            val result = backupUseCase.backupToJson()
            _uiState.value = when (result) {
                is Result.Success -> BackupUiState.ReadyToShare(result.data)
                is Result.Error -> BackupUiState.Error(result.exception?.message ?: "Lỗi backup JSON")
                else -> BackupUiState.Error("Lỗi không xác định")
            }
        }
    }

    fun backupDb() {
        viewModelScope.launch {
            _uiState.value = BackupUiState.Loading
            val result = backupUseCase.backupDb()
            _uiState.value = when (result) {
                is Result.Success -> BackupUiState.ReadyToShare(result.data)
                is Result.Error -> BackupUiState.Error(result.exception?.message ?: "Lỗi backup DB")
                else -> BackupUiState.Error("Lỗi không xác định")
            }
        }
    }

    fun previewRestore(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = BackupUiState.Loading
            val result = restoreUseCase.previewJson(uri)
            _uiState.value = when (result) {
                is Result.Success -> BackupUiState.RestorePreview(result.data, uri)
                is Result.Error -> BackupUiState.Error(result.exception?.message ?: "File backup không hợp lệ")
                else -> BackupUiState.Error("Lỗi không xác định")
            }
        }
    }

    fun confirmRestore(uri: Uri, overwrite: Boolean) {
        viewModelScope.launch {
            _uiState.value = BackupUiState.Restoring
            val result = restoreUseCase.restoreJson(uri, overwrite)
            _uiState.value = when (result) {
                is Result.Success -> BackupUiState.RestoreSuccess(result.data.deckCount, result.data.cardCount)
                is Result.Error -> BackupUiState.Error(result.exception?.message ?: "Lỗi restore")
                else -> BackupUiState.Error("Lỗi không xác định")
            }
        }
    }

    fun resetBackup() { _uiState.value = BackupUiState.Idle }
}
