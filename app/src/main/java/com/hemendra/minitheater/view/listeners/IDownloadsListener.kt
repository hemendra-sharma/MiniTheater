package com.hemendra.minitheater.view.listeners

import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.presenter.DownloadFailureReason

interface IDownloadsListener {

    fun onDownloadAdded(movie: Movie)
    fun onDownloadRemoved(movie: Movie)
    fun failedToAddDownload(reason: DownloadFailureReason)
    fun onDownloadProgress(movie: Movie, progress: Float)

}