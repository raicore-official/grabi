package com.raisoft.grabi.data.download

import com.raisoft.grabi.domain.model.MediaOption
import com.yausername.youtubedl_android.YoutubeDL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DownloadStatus { RUNNING, DONE, FAILED, CANCELED }

data class DownloadJob(
    val id: String,
    val title: String,
    val optionLabel: String,
    val status: DownloadStatus,
    val progress: Float = 0f,
    val etaSeconds: Long = -1,
    val filePath: String? = null,
    val error: String? = null,
)

@Singleton
class DownloadController @Inject constructor(
    private val engine: YtDlpEngine,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow<DownloadJob?>(null)
    val state: StateFlow<DownloadJob?> = _state.asStateFlow()

    fun start(url: String, title: String, option: MediaOption, subtitles: Boolean) {
        if (_state.value?.status == DownloadStatus.RUNNING) return
        val id = UUID.randomUUID().toString()
        _state.value = DownloadJob(id, title, option.label, DownloadStatus.RUNNING)
        scope.launch {
            try {
                val file = engine.download(url, option, subtitles, id) { percent, eta ->
                    _state.update {
                        it?.copy(progress = percent.coerceIn(0f, 100f) / 100f, etaSeconds = eta)
                    }
                }
                _state.update { it?.copy(status = DownloadStatus.DONE, progress = 1f, filePath = file.absolutePath) }
            } catch (e: YoutubeDL.CanceledException) {
                _state.update { it?.copy(status = DownloadStatus.CANCELED) }
            } catch (e: Exception) {
                _state.update { it?.copy(status = DownloadStatus.FAILED, error = e.message ?: "Download failed") }
            }
        }
    }

    fun cancel() {
        _state.value?.takeIf { it.status == DownloadStatus.RUNNING }?.let { engine.cancel(it.id) }
    }

    fun dismiss() {
        if (_state.value?.status != DownloadStatus.RUNNING) _state.value = null
    }
}