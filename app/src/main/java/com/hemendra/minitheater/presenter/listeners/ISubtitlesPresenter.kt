package com.hemendra.minitheater.presenter.listeners

import com.hemendra.minitheater.data.Movie

interface ISubtitlesPresenter {
    fun getSubtitlesList(movie:Movie, listener: SubtitlesListDownloadListener)
    fun getSubtitle(path: String, listener: SubtitleDownloadListener)
}