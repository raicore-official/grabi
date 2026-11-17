package com.raisoft.grabi.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raisoft.grabi.data.db.FavoriteEntity
import com.raisoft.grabi.data.db.HistoryEntity
import com.raisoft.grabi.data.db.LibraryDao
import com.raisoft.grabi.data.download.DownloadManager
import com.raisoft.grabi.data.db.DownloadEntity
import com.raisoft.grabi.domain.model.DownloadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibrarySection { HISTORY, FAVORITES, DOWNLOADS }

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryDao: LibraryDao,
    downloadManager: DownloadManager,
) : ViewModel() {

    private val _section = MutableStateFlow(LibrarySection.HISTORY)
    val section = _section.asStateFlow()

    val history = libraryDao.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favorites = libraryDao.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val downloads = downloadManager.downloads
        .map { list -> list.filter { it.status == DownloadStatus.DONE.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSection(section: LibrarySection) {
        _section.value = section
    }

    fun clearHistory() {
        viewModelScope.launch { libraryDao.clearHistory() }
    }

    fun removeHistory(entry: HistoryEntity) {
        viewModelScope.launch { libraryDao.deleteHistory(entry.url) }
    }

    fun removeFavorite(entry: FavoriteEntity) {
        viewModelScope.launch { libraryDao.deleteFavorite(entry.url) }
    }

    fun playableTarget(entry: DownloadEntity): String? = entry.contentUri ?: entry.filePath
}