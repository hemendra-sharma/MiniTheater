package com.hemendra.minitheater.presenter.listeners

import android.content.Context
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.Torrent
import com.hemendra.minitheater.presenter.DownloadFailureReason
import java.io.File

interface IDownloadsPresenter {

    fun checkAndStartOngoingDownload(context: Context)
    fun checkAndStartOngoingStream(context: Context)
    fun addDownload(movie: Movie): DownloadFailureReason
    fun removeDownload(movie: Movie): Boolean
    fun getDownloadsList(): ArrayList<Movie>
    fun startDownload(context: Context, movie: Movie, stopOngoing: Boolean): DownloadFailureReason
    fun pauseOrResumeDownload(context: Context, movie: Movie): Boolean
    fun stopDownload(context: Context, movie: Movie): Boolean
    fun updateMovie(movie: Movie)
    fun getTorrentFile(torrent: Torrent): File?

}