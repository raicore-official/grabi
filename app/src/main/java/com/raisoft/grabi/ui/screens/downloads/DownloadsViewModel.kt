package com.raisoft.grabi.ui.screens.downloads

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raisoft.grabi.data.db.DownloadEntity
import com.raisoft.grabi.data.download.DownloadManager
import com.raisoft.grabi.domain.DownloadFilter
import com.raisoft.grabi.domain.model.DownloadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val manager: DownloadManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _filter = MutableStateFlow(DownloadFilter.ALL)
    val filter: StateFlow<DownloadFilter> = _filter.asStateFlow()

    val downloads: StateFlow<List<DownloadEntity>> =
        combine(manager.downloads, _filter) { list, filter ->
            list.filter { filter.matches(DownloadStatus.valueOf(it.status)) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(filter: DownloadFilter) {
        _filter.value = filter
    }

    fun pause(id: String) = manager.pause(id)

    fun resume(id: String) = manager.resume(id)

    fun retry(id: String) = manager.retry(id)

    fun cancel(id: String) = manager.cancel(id)

    fun delete(id: String) = manager.delete(id)

    fun open(entity: DownloadEntity) {
        val location = entity.contentUri ?: entity.filePath ?: return
        val uri = if (location.startsWith("content:")) {
            Uri.parse(location)
        } else {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(location))
        }
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}