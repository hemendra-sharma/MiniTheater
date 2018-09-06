package com.hemendra.minitheater.presenter.listeners

import android.content.Context
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.Torrent
import com.hemendra.minitheater.presenter.DownloadFailureReason
import java.io.File

interface IDownloadsPresenter {

    fun checkAndStartOngoingDownload(context: Context)
    fun addDownload(movie: Movie): DownloadFailureReason
    fun removeDownload(movie: Movie): Boolean
    fun getDownloadsList(): ArrayList<Movie>
    fun startDownload(context: Context, movie: Movie): DownloadFailureReason
    fun pauseDownload(context: Context, movie: Movie): Boolean
    fun updateDownloadProgress(movie: Movie)
    fun getTorrentFile(torrent: Torrent): File?

}