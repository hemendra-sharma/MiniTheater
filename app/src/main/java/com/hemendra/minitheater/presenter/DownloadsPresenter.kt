package com.hemendra.minitheater.presenter

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
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
                    startDownload(context, movie)
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

    override fun startDownload(context: Context, movie: Movie): DownloadFailureReason {
        return if(DownloaderService.isRunning) {
            DownloadFailureReason.ALREADY_DOWNLOADING
        } else if(downloadsList?.startDownload(movie) == true) {
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

    override fun pauseDownload(context: Context, movie: Movie): Boolean {
        return if(!DownloaderService.isRunning) {
            false
        } else if(downloadsList?.pauseDownload(movie) == true) {
            saveState()
            context.stopService(Intent(context, DownloaderService::class.java))
        } else false
    }

    override fun updateDownloadProgress(movie: Movie) {
        if(downloadsList?.update(movie) == true) {
            saveState()
        }
    }

    override fun getTorrentFile(torrent: Torrent): File? {
        val dir = File(dir.absolutePath+"/"+torrent.hash)
        val subDirs = dir.listFiles()
        if(subDirs != null
                && subDirs.isNotEmpty() && subDirs[0].isDirectory) {
            val files = subDirs[0].listFiles()
            if(files != null
                    && files.isNotEmpty() && files[0].isFile)
                return files[0]
        }
        return null
    }

    private fun saveState() {
        downloadsList?.let { Utils.writeToFile(it, downloadsListFile) }
    }
}