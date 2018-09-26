package com.hemendra.minitheater.view.downloader

import android.content.Intent
import android.os.Bundle
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
import com.hemendra.minitheater.presenter.ImagesPresenter
import com.hemendra.minitheater.view.listeners.OnDownloadItemClickListener
import com.hemendra.minitheater.view.listeners.OnMovieDownloadClickListener
import com.hemendra.minitheater.view.showMessage
import com.hemendra.minitheater.view.showYesNoMessage
import kotlinx.android.synthetic.main.fragment_downloader.*

class DownloaderFragment: Fragment() {

    private val downloadsPresenter = DownloadsPresenter.getInstance()
    private var adapter: DownloadsListAdapter? = null

    private var savedView: View? = null

    private var movieToAdd: Movie? = null
    fun setMovieToAdd(movie: Movie) {
        movieToAdd = movie
    }

    private var onMovieDownloadClickListener: OnMovieDownloadClickListener? = null
    fun setMovieClickListener(onMovieDownloadClickListener: OnMovieDownloadClickListener) {
        this.onMovieDownloadClickListener = onMovieDownloadClickListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        savedView?.let { return it }
        savedView = inflater.inflate(R.layout.fragment_downloader, container, false)
        return savedView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        context?.let {
            DownloadsPresenter.getInstance().checkAndStartOngoingDownload(it)
        }
        loadFreshList()
        movieToAdd?.let {
            addDownload(it)
            movieToAdd = null
        }
    }

    override fun onDestroyView() {
        context?.let { ImagesPresenter.getInstance(it).abortAll() }
        (savedView?.parent as ViewGroup?)?.removeAllViews()
        super.onDestroyView()
    }

    fun destroy() {
        savedView = null
    }

    fun loadFreshList() {
        try {
            adapter?.let {
                it.movies = downloadsPresenter.getDownloadsList()
                it.notifyDataSetChanged()
                return
            }
            adapter = DownloadsListAdapter(downloadsPresenter.getDownloadsList(),
                    onDownloadItemClickListener)
            recycler?.let { it.adapter = adapter }
        } finally {
            recycler?.let { r ->
                r.adapter?.let { a ->
                    if(a.itemCount > 0)
                        tvNoDownloads?.visibility = View.GONE
                    else
                        tvNoDownloads?.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun addDownload(movie: Movie) {
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
            if(file != null && !file.absolutePath.endsWith(".mp4")) {
                onExternalClicked(movie)
            } else {
                context?.let {
                    onMovieDownloadClickListener?.onPlayClicked(movie)
                }
            }
        }

        override fun onPauseClicked(movie: Movie) {
            context?.let {
                if(movie.isDownloading) {
                    if(!downloadsPresenter.pauseOrResumeDownload(it, movie))
                        showMessage(it, "Failed to Pause/Resume Download!")
                } else {
                    val ret = downloadsPresenter.startDownload(it, movie, false)
                    if(ret == DownloadFailureReason.ALREADY_DOWNLOADING)
                        showMessage(it, "Already Downloading a Movie! It " +
                                "Can Download Only 1 Movie at a Time.")
                    else if(ret != DownloadFailureReason.NONE)
                        showMessage(it, "Failed to Start the Download! ($ret)")
                }
            }
        }

        override fun onStopClicked(movie: Movie) {
            context?.let {
                if(!downloadsPresenter.stopDownload(it, movie)) {
                    showMessage(it, "Failed to Stop Download!")
                }
            }
        }

        override fun onDeleteClicked(movie: Movie) {
            context?.let {
                if(movie.isDownloading) {
                    showMessage(it, "Cannot remove an ongoing download. Stop the download first.")
                } else {
                    val msg = """The downloaded movie data will be deleted.
                        |Are you sure you want to delete the movie "${movie.title}"?""".trimMargin()
                    showYesNoMessage(it, msg, onYesClicked = Runnable {
                        downloadsPresenter.stopDownload(it, movie)
                        downloadsPresenter.removeDownload(movie)
                        loadFreshList()
                    })
                }
            }
        }

        override fun onExternalClicked(movie: Movie) {
            val file = downloadsPresenter.getTorrentFile(movie.torrents[0])
            if(file != null) {
                val tenMB = 10L * 1024L * 1024L
                if (file.length() > tenMB) {
                    context?.let {
                        val intent = Intent(Intent.ACTION_VIEW)
                        val uri = FileProvider.getUriForFile(it, it.packageName + ".provider", file)
                        intent.setDataAndType(uri, "video/*")
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        startActivity(intent)
                    }
                } else {
                    context?.let {
                        showMessage(it, "No data to play! Download at least 10 MB to start playing")
                    }
                }
            } else {
                context?.let {
                    showMessage(it, "No data to play! Download at least 10 MB to start playing")
                }
            }
        }
    }

}