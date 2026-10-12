package com.jhaiian.clint.downloads

enum class DownloadsTabType {
    ALL, DOWNLOADING, FINISHED, ERROR;

    companion object {
        val ACTIVE_STATUSES: Set<DownloadStatus> = setOf(
            DownloadStatus.QUEUED,
            DownloadStatus.CONNECTING,
            DownloadStatus.ALLOCATING,
            DownloadStatus.DOWNLOADING,
            DownloadStatus.RETRYING,
            DownloadStatus.COPYING_TEMP,
            DownloadStatus.DELETING_TEMP,
            DownloadStatus.PAUSED
        )
    }
}
