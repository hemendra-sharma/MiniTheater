package com.hemendra.minitheater.presenter

import android.content.Context
import android.content.Intent
import android.os.Environment
import com.hemendra.minitheater.data.DownloadsList
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.Torrent
import com.hemendra.minitheater.presenter.listeners.IDownloadsPresenter
import com.hemendra.minitheater.service.DownloaderService
import com.hemendra.minitheater.utils.Utils
import java.io.File

class DownloadsPresenter private constructor():
        IDownloadsPresenter {

    companion object {
        private var instance : DownloadsPresenter? = null
        fun getInstance() : DownloadsPresenter {
            if(instance == null) instance = DownloadsPresenter()
            return instance!!
        }
    }

    private val dirPath = """${Environment.getExternalStorageDirectory().absolutePath}/MiniTheater"""
    private val dir = File(dirPath)
    init { dir.mkdirs() }

    private val downloadsListFile = File(dir, "downloadingMovies.obj")
    private var downloadsList: DownloadsList? = null
    init {
        downloadsList = Utils.readObjectFromFile(downloadsListFile) as DownloadsList?
        if(downloadsList == null) downloadsList = DownloadsList()
    }

    override fun checkAndStartOngoingDownload(context: Context) {
        downloadsList?.let {
            for(movie in it.movies) {
                if(movie.isDownloading) {
                    startDownload(context, movie, false)
                    break
                }
            }
        }
    }

    override fun addDownload(movie: Movie): DownloadFailureReason {
        assert(movie.torrents.size == 1)

        if(movie.torrents[0].size_bytes > Utils.getAvailableSpace()) {
            return DownloadFailureReason.NOT_ENOUGH_SPACE
        } else if(downloadsList?.add(movie) == true) {
            saveState()
            return DownloadFailureReason.NONE
        } else {
            return DownloadFailureReason.ALREADY_ADDED
        }
    }

    override fun removeDownload(movie: Movie): Boolean {
        if(downloadsList?.remove(movie) == true) {
            saveState()
            val dir = File(dir.absolutePath+"/"+movie.torrents[0].hash)
            if(dir.exists()) {
                if(dir.isFile) Utils.deleteFile(dir)
                else if(dir.isDirectory) Utils.deleteDirectory(dir)
            }
            return true
        }
        return false
    }

    override fun getDownloadsList(): ArrayList<Movie> {
        downloadsList?.let {
            return it.movies
        }
        return ArrayList()
    }

    override fun startDownload(context: Context,
                               movie: Movie, stopOngoing: Boolean): DownloadFailureReason {
        return if(!stopOngoing && DownloaderService.isRunning) {
            DownloadFailureReason.ALREADY_DOWNLOADING
        } else if(downloadsList?.startDownload(movie) == true) {
            if(stopOngoing) {
                context.stopService(Intent(context, DownloaderService::class.java))
            }
            saveState()
            val intent = Intent(context, DownloaderService::class.java)
            intent.putExtra(DownloaderService.EXTRA_MOVIE, movie)
            val ret = context.startService(intent)
            if(ret != null)
                DownloadFailureReason.NONE
            else
                DownloadFailureReason.UNKNOWN
        } else {
            DownloadFailureReason.UNKNOWN
        }
    }

    override fun pauseOrResumeDownload(context: Context, movie: Movie): Boolean {
        return if(!DownloaderService.isRunning) {
            false
        } else if(downloadsList?.pauseOrResumeDownload(movie) == true) {
            saveState()
            val intent = Intent(context, DownloaderService::class.java)
            intent.putExtra("action", "Pause")
            context.startService(intent)
            true
        } else false
    }

    override fun stopDownload(context: Context, movie: Movie): Boolean {
        return if(!DownloaderService.isRunning) {
            false
        } else if(downloadsList?.stopDownload(movie) == true) {
            saveState()
            context.stopService(Intent(context, DownloaderService::class.java))
        } else false
    }

    override fun updateMovie(movie: Movie) {
        if(downloadsList?.update(movie) == true) {
            saveState()
        }
    }

    override fun getTorrentFile(torrent: Torrent): File? {
        val dir = File(dir.absolutePath+"/"+torrent.hash)
        return getTorrentFile(dir)
    }

    private fun getTorrentFile(file: File): File? {
        if(file.isFile && file.length() > 0) {
            return file
        }
        //
        if(file.exists() && file.isDirectory) {
            val files = file.listFiles()
            files?.let { subFiles ->
                for(subFile in subFiles) {
                    val f = getTorrentFile(subFile)
                    if(f != null) return f
                }
            }
        }
        return null
    }

    private fun saveState() {
        downloadsList?.let { Utils.writeToFile(it, downloadsListFile) }
    }
}