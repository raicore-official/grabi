package com.raisoft.grabi.domain

import com.raisoft.grabi.domain.model.DownloadStatus

enum class DownloadFilter {
    ALL,
    ACTIVE,
    DONE,
    FAILED;

    fun matches(status: DownloadStatus): Boolean = when (this) {
        ALL -> true
        ACTIVE -> status == DownloadStatus.QUEUED ||
            status == DownloadStatus.RUNNING ||
            status == DownloadStatus.PAUSED ||
            status == DownloadStatus.WAITING_WIFI
        DONE -> status == DownloadStatus.DONE
        FAILED -> status == DownloadStatus.FAILED || status == DownloadStatus.CANCELED
    }
}