package com.dttrn.datfs.feature.importexport.presentation

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dttrn.datfs.core.data.local.dao.DeckDao
import com.dttrn.datfs.core.data.local.entity.DeckEntity
import com.dttrn.datfs.core.domain.common.Result
import com.dttrn.datfs.feature.importexport.domain.usecase.ExportExcelUseCase
import com.dttrn.datfs.feature.importexport.domain.usecase.ImportExcelUseCase
import com.dttrn.datfs.feature.importexport.domain.usecase.ImportPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class ImportExportTab { IMPORT, EXPORT }

sealed class ImportUiState {
    data object Idle : ImportUiState()
    data object Parsing : ImportUiState()
    data class Preview(val preview: ImportPreview) : ImportUiState()
    data object Importing : ImportUiState()
    data class Success(val count: Int) : ImportUiState()
    data class Error(val message: String) : ImportUiState()
}

sealed class ExportUiState {
    data object Idle : ExportUiState()
    data object Exporting : ExportUiState()
    data class ReadyToShare(val intent: Intent) : ExportUiState()
    data class Error(val message: String) : ExportUiState()
}

@HiltViewModel
class ImportExportViewModel @Inject constructor(
    private val importUseCase: ImportExcelUseCase,
    private val exportUseCase: ExportExcelUseCase,
    private val deckDao: DeckDao,
) : ViewModel() {

    private val _tab = MutableStateFlow(ImportExportTab.IMPORT)
    val tab: StateFlow<ImportExportTab> = _tab.asStateFlow()

    private val _importState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val importState: StateFlow<ImportUiState> = _importState.asStateFlow()

    private val _exportState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    private val _decks = MutableStateFlow<List<DeckEntity>>(emptyList())
    val decks: StateFlow<List<DeckEntity>> = _decks.asStateFlow()

    private val _selectedDeckId = MutableStateFlow<String?>(null)
    val selectedDeckId: StateFlow<String?> = _selectedDeckId.asStateFlow()

    init {
        loadDecks()
    }

    fun setTab(tab: ImportExportTab) { _tab.value = tab }

    fun selectDeck(deckId: String?) { _selectedDeckId.value = deckId }

    fun parseFile(uri: Uri, context: android.content.Context) {
        viewModelScope.launch {
            _importState.value = ImportUiState.Parsing
            try {
                val stream = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                        ?: throw Exception("Không thể mở file")
                }
                val result = importUseCase.preview(
                    inputStream = stream,
                    targetDeckId = _selectedDeckId.value,
                    newDeckTitle = extractFileName(context, uri),
                )
                _importState.value = when (result) {
                    is Result.Success -> ImportUiState.Preview(result.data)
                    is Result.Error -> ImportUiState.Error(result.exception?.message ?: "Lỗi parse file")
                    else -> ImportUiState.Error("Lỗi không xác định")
                }
            } catch (e: Exception) {
                _importState.value = ImportUiState.Error(e.message ?: "Lỗi khi đọc file")
            }
        }
    }

    fun confirmImport() {
        val preview = (_importState.value as? ImportUiState.Preview)?.preview ?: return
        viewModelScope.launch {
            _importState.value = ImportUiState.Importing
            val result = importUseCase.confirm(preview)
            _importState.value = when (result) {
                is Result.Success -> ImportUiState.Success(result.data)
                is Result.Error -> ImportUiState.Error(result.exception?.message ?: "Lỗi import")
                else -> ImportUiState.Error("Lỗi không xác định")
            }
            loadDecks() // Refresh deck list
        }
    }

    fun resetImport() { _importState.value = ImportUiState.Idle }

    fun exportAll() {
        viewModelScope.launch {
            _exportState.value = ExportUiState.Exporting
            val result = exportUseCase.exportAll()
            _exportState.value = when (result) {
                is Result.Success -> ExportUiState.ReadyToShare(result.data)
                is Result.Error -> ExportUiState.Error(result.exception?.message ?: "Lỗi export")
                else -> ExportUiState.Error("Lỗi không xác định")
            }
        }
    }

    fun exportSelected() {
        val deckId = _selectedDeckId.value ?: return exportAll()
        viewModelScope.launch {
            _exportState.value = ExportUiState.Exporting
            val result = exportUseCase.exportDeck(deckId)
            _exportState.value = when (result) {
                is Result.Success -> ExportUiState.ReadyToShare(result.data)
                is Result.Error -> ExportUiState.Error(result.exception?.message ?: "Lỗi export")
                else -> ExportUiState.Error("Lỗi không xác định")
            }
        }
    }

    fun exportTemplate() {
        viewModelScope.launch {
            _exportState.value = ExportUiState.Exporting
            val result = exportUseCase.exportTemplate()
            _exportState.value = when (result) {
                is Result.Success -> ExportUiState.ReadyToShare(result.data)
                is Result.Error -> ExportUiState.Error(result.exception?.message ?: "Lỗi tạo template")
                else -> ExportUiState.Error("Lỗi không xác định")
            }
        }
    }

    fun resetExport() { _exportState.value = ExportUiState.Idle }

    private fun loadDecks() {
        viewModelScope.launch {
            _decks.value = deckDao.getActiveDecks().first()
        }
    }

    private fun extractFileName(context: android.content.Context, uri: Uri): String {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIdx).substringBeforeLast(".")
            } ?: "Deck nhập khẩu"
        } catch (e: Exception) {
            "Deck nhập khẩu"
        }
    }
}
