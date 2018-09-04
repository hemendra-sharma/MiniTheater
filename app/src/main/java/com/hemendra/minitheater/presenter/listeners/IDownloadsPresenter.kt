package com.hemendra.minitheater.presenter.listeners

import com.hemendra.minitheater.data.Movie

interface IDownloadsPresenter {

    fun addDownload(movie: Movie)
    fun removeDownload(movie: Movie)
    fun getDownloadsList(): ArrayList<Movie>
    fun startDownload(movie: Movie)
    fun pauseDownload(movie: Movie)

}