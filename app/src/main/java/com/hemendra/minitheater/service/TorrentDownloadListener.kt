package com.hemendra.minitheater.service

interface TorrentDownloadListener {

    fun onProgressUpdate(progress: Float)
    fun onDownloadComplete()
    fun onDownloadFailed(reason: TorrentFailureReason)

}