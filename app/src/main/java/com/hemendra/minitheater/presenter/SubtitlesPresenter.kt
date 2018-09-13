package com.hemendra.minitheater.presenter

import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.model.subtitles.SubtitleDownloader
import com.hemendra.minitheater.data.model.subtitles.SubtitlesListDownloader
import com.hemendra.minitheater.presenter.listeners.ISubtitlesPresenter
import com.hemendra.minitheater.presenter.listeners.SubtitleDownloadListener
import com.hemendra.minitheater.presenter.listeners.SubtitlesListDownloadListener

class SubtitlesPresenter private constructor(): ISubtitlesPresenter {

    companion object {
        private val instance = SubtitlesPresenter()
        fun getInstance(): SubtitlesPresenter = instance
    }

    private var subtitlesListDownloader: SubtitlesListDownloader? = null
    private var subtitleDownloader: SubtitleDownloader? = null

    override fun getSubtitlesList(movie: Movie, listener: SubtitlesListDownloadListener) {
        if(subtitlesListDownloader != null && subtitlesListDownloader?.isExecuting == true)
            return
        subtitlesListDownloader = SubtitlesListDownloader(listener)
        subtitlesListDownloader?.execute(movie)
    }

    override fun getSubtitle(path: String, listener: SubtitleDownloadListener) {
        if(subtitleDownloader != null && subtitleDownloader?.isExecuting == true)
            subtitleDownloader?.cancel(true)

        subtitleDownloader = SubtitleDownloader(listener)
        subtitleDownloader?.execute(path)
    }

}