package com.hemendra.minitheater.view.downloader

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.presenter.DownloadFailureReason
import com.hemendra.minitheater.view.listeners.IDownloadsListener

class DownloaderFragment: Fragment(), IDownloadsListener {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_downloader, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

    }

    override fun onDownloadAdded(movie: Movie) {

    }

    override fun onDownloadRemoved(movie: Movie) {

    }

    override fun failedToAddDownload(reason: DownloadFailureReason) {

    }

    override fun onDownloadProgress(movie: Movie, progress: Float) {

    }
}