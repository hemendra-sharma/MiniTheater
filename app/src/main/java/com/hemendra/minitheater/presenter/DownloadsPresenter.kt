package com.hemendra.minitheater.presenter

import android.os.Environment
import com.hemendra.minitheater.data.DownloadsList
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.presenter.listeners.IDownloadsPresenter
import com.hemendra.minitheater.utils.Utils
import com.hemendra.minitheater.view.listeners.IDownloadsListener
import java.io.File

class DownloadsPresenter private constructor(private var listener: IDownloadsListener):
        IDownloadsPresenter {

    companion object {
        private var instance : DownloadsPresenter? = null
        fun getInstance(listener: IDownloadsListener) : DownloadsPresenter {
            if(instance == null) instance = DownloadsPresenter(listener)
            instance?.listener = listener
            return instance!!
        }
    }

    private val dirPath = """${Environment.getExternalStorageDirectory().absolutePath}
        |${File.pathSeparator}Movies""".trimMargin()
    private val dir = File(dirPath)
    init { dir.mkdirs() }

    private val downloadsListFile = File(dir, "downloadingMovies.obj")
    private var downloadsList: DownloadsList? = null
    init {
        downloadsList = Utils.readObjectFromFile(downloadsListFile) as DownloadsList?
        if(downloadsList == null) downloadsList = DownloadsList()
    }

    override fun addDownload(movie: Movie) {
        assert(movie.torrents.size == 1)

        if(movie.torrents[0].size_bytes > Utils.getAvailableSpace()) {
            listener.failedToAddDownload(DownloadFailureReason.NOT_ENOUGH_SPACE)
        } else if(downloadsList?.add(movie) == true) {
            saveState()
            listener.onDownloadAdded(movie)
        }
    }

    override fun removeDownload(movie: Movie) {
        if(downloadsList?.remove(movie) == true) {
            saveState()
            listener.onDownloadRemoved(movie)
        }
    }

    override fun getDownloadsList(): ArrayList<Movie> {
        downloadsList?.let {
            return it.movies
        }
        return ArrayList()
    }

    override fun startDownload(movie: Movie) {
        movie.isDownloading = true
    }

    override fun pauseDownload(movie: Movie) {
        movie.isDownloading = false
    }

    private fun saveState() {
        downloadsList?.let { Utils.writeToFile(it, downloadsListFile) }
    }
}