package com.hemendra.minitheater.presenter.listeners

import java.io.File

interface SubtitleDownloadListener {
    fun onSubtitleDownloaded(subtitleFile: File)
    fun onFailedToDownload()
}