package com.hemendra.minitheater.view.downloader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import android.support.v4.content.FileProvider
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.presenter.DownloadFailureReason
import com.hemendra.minitheater.presenter.DownloadsPresenter
import com.hemendra.minitheater.view.listeners.OnDownloadItemClickListener
import com.hemendra.minitheater.view.showMessage
import com.hemendra.minitheater.view.showYesNoMessage
import kotlinx.android.synthetic.main.fragment_downloader.*
import java.io.File

class DownloaderFragment: Fragment() {

    private val downloadsPresenter = DownloadsPresenter.getInstance()
    private var adapter: DownloadsListAdapter? = null

    private var movieToAdd: Movie? = null
    fun setMovieToAdd(movie: Movie) {
        movieToAdd = movie
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Log.i("fragment", "onCreateView")
        return inflater.inflate(R.layout.fragment_downloader, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.i("fragment", "onViewCreated")
        context?.let {
            DownloadsPresenter.getInstance().checkAndStartOngoingDownload(it)
        }
        loadFreshList()
        movieToAdd?.let {
            addDownload(it)
            movieToAdd = null
        }
    }

    private fun loadFreshList() {
        try {
            adapter?.let {
                it.movies = downloadsPresenter.getDownloadsList()
                it.notifyDataSetChanged()
                return
            }
            adapter = DownloadsListAdapter(downloadsPresenter.getDownloadsList(),
                    onDownloadItemClickListener)
            recycler?.let { it.adapter = adapter }
        }finally {
            recycler?.let { r ->
                r.adapter?.let { a ->
                    if(a.itemCount > 0)
                        tvNoDownloads.visibility = View.GONE
                    else
                        tvNoDownloads.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun addDownload(movie: Movie) {
        Log.i("fragment", "addDownload")
        val reason = downloadsPresenter.addDownload(movie)
        when (reason) {
            DownloadFailureReason.NONE -> {
                loadFreshList()
            }
            DownloadFailureReason.NOT_ENOUGH_SPACE -> {
                context?.let {
                    showMessage(it, "Not Enough Free Space on Device to Download this Movie!")
                }
            }
            else -> context?. let {
                showMessage(it, "This Movie is Already in Downloads List!")
            }
        }
    }

    private val onDownloadItemClickListener = object: OnDownloadItemClickListener {

        override fun onItemClicked(movie: Movie) {
            // play movie in video player
            val file = downloadsPresenter.getTorrentFile(movie.torrents[0])
            if(file != null) {
                context?.let {
                    val intent = Intent(Intent.ACTION_VIEW)
                    val uri = FileProvider.getUriForFile(it, it.packageName + ".provider", file)
                    intent.setDataAndType(uri, "video/mp4")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(intent)
                }
            }
        }

        override fun onPauseClicked(movie: Movie) {
            context?.let {
                if(!movie.isDownloading) {
                    val ret = downloadsPresenter.startDownload(it, movie)
                    if(ret == DownloadFailureReason.ALREADY_DOWNLOADING)
                        showMessage(it, "Already Downloading a Movie! It " +
                                "Can Download Only 1 Movie at a Time.")
                    else if(ret != DownloadFailureReason.NONE)
                        showMessage(it, "Failed to Start Download!")
                } else if(!downloadsPresenter.pauseDownload(it, movie)) {
                    showMessage(it, "Failed to Stop Download!")
                }
                adapter?.notifyDataSetChanged()
            }
        }
        override fun onDeleteClicked(movie: Movie) {
            context?.let {
                if(movie.isDownloading) {
                    showMessage(it, "Cannot remove an ongoing download. Stop the download first.")
                } else {
                    val msg = """Are you sure you want to remove "${movie.title}" from downloads?"""
                    showYesNoMessage(it, msg, Runnable {
                        downloadsPresenter.pauseDownload(it, movie)
                        downloadsPresenter.removeDownload(movie)
                        loadFreshList()
                    })
                }
            }
        }
    }

}