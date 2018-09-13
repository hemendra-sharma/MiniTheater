package com.hemendra.minitheater.presenter.listeners

import com.hemendra.minitheater.data.Subtitle

interface SubtitlesListDownloadListener {
    fun onListDownloaded(subtitlesList: ArrayList<Subtitle>)
    fun onFailedToDownloadList();
}