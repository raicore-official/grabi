package com.raisoft.grabi.domain

import com.raisoft.grabi.domain.model.DownloadStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadFilterTest {

    @Test
    fun allMatchesEveryStatus() {
        DownloadStatus.entries.forEach {
            assertTrue(DownloadFilter.ALL.matches(it))
        }
    }

    @Test
    fun activeCoversQueueAndPausedStates() {
        listOf(
            DownloadStatus.QUEUED,
            DownloadStatus.RUNNING,
            DownloadStatus.PAUSED,
            DownloadStatus.WAITING_WIFI,
        ).forEach { assertTrue(DownloadFilter.ACTIVE.matches(it)) }
        assertFalse(DownloadFilter.ACTIVE.matches(DownloadStatus.DONE))
        assertFalse(DownloadFilter.ACTIVE.matches(DownloadStatus.FAILED))
    }

    @Test
    fun doneMatchesOnlyCompleted() {
        assertTrue(DownloadFilter.DONE.matches(DownloadStatus.DONE))
        assertFalse(DownloadFilter.DONE.matches(DownloadStatus.RUNNING))
    }

    @Test
    fun failedIncludesCanceled() {
        assertTrue(DownloadFilter.FAILED.matches(DownloadStatus.FAILED))
        assertTrue(DownloadFilter.FAILED.matches(DownloadStatus.CANCELED))
        assertFalse(DownloadFilter.FAILED.matches(DownloadStatus.PAUSED))
    }
}