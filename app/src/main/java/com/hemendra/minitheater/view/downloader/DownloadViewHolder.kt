package com.hemendra.minitheater.view.downloader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.presenter.DownloadsPresenter
import com.hemendra.minitheater.presenter.ImagesPresenter
import com.hemendra.minitheater.service.DownloaderService
import com.hemendra.minitheater.view.listeners.ImageLoaderCallback
import com.hemendra.minitheater.view.listeners.OnDownloadItemClickListener
import java.util.*

class DownloadViewHolder(private var view: View, private val listener: OnDownloadItemClickListener,
                         private val imagesPresenter: ImagesPresenter):
        RecyclerView.ViewHolder(view), ImageLoaderCallback {

    private val ivCover: ImageView = view.findViewById(R.id.ivCover)
    private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
    private val tvQuality: TextView = view.findViewById(R.id.tvQuality)
    private val tvSize: TextView = view.findViewById(R.id.tvSize)
    private val tvProgress: TextView = view.findViewById(R.id.tvProgress)
    private val tvSeeds: TextView = view.findViewById(R.id.tvSeeds)
    private val tvDownloadSpeed: TextView = view.findViewById(R.id.tvDownloadSpeed)
    private val tvUploadSpeed: TextView = view.findViewById(R.id.tvUploadSpeed)
    private val ivExternalVideo: ImageView = view.findViewById(R.id.ivExternalVideo)
    private val ivPause: ImageView = view.findViewById(R.id.ivPause)
    private val ivStop: ImageView = view.findViewById(R.id.ivStop)
    private val ivDelete: ImageView = view.findViewById(R.id.ivDelete)
    var movie: Movie? = null

    private var loadingImageURL = ""

    fun fillDetails(m: Movie) {
        movie = m
        ivCover.setImageBitmap(null)
        loadingImageURL = imagesPresenter.loadCoverImage(m, this)
        tvTitle.text = m.title

        updateProgress(m)

        view.setOnClickListener{
            listener.onItemClicked(m)
        }
        ivExternalVideo.setOnClickListener {
            listener.onExternalClicked(m)
        }
        ivPause.setOnClickListener {
            saveProgress()
            listener.onPauseClicked(m)
            updateProgress(m)
        }
        ivStop.setOnClickListener {
            saveProgress()
            listener.onStopClicked(m)
            updateProgress(m)
        }
        ivDelete.setOnClickListener {
            listener.onDeleteClicked(m)
        }
    }

    private fun updateProgress(m: Movie) {
        movie?.downloadProgress = m.downloadProgress
        movie?.isDownloading = m.isDownloading
        movie?.downloadSpeed = m.downloadSpeed
        movie?.uploadSpeed = m.uploadSpeed
        movie?.downloadSeeds = m.downloadSeeds
        movie?.isDownloading = m.isDownloading
        movie?.isPaused = m.isPaused

        if(movie?.torrents?.get(0)?.size?.isEmpty() == true) {
            movie?.torrents?.get(0)?.size = m.torrents[0].size
            movie?.torrents?.get(0)?.size_bytes = m.torrents[0].size_bytes
            saveProgress()
        }

        if(m.torrents[0].quality.isNotEmpty())
            tvQuality.text = String.format(Locale.getDefault(), "Quality: %s",
                    m.torrents[0].quality)
        else
            tvQuality.visibility = View.GONE

        if(m.torrents[0].size.isNotEmpty())
            tvSize.text = String.format(Locale.getDefault(), "Size: %s", m.torrents[0].size)
        else
            tvSize.visibility = View.GONE

        tvDownloadSpeed.text = String.format(Locale.getDefault(),
                "Down: %.1f KB/s", m.downloadSpeed.toFloat() / 1024f)
        tvUploadSpeed.text = String.format(Locale.getDefault(),
                "Up: %.1f KB/s", m.uploadSpeed.toFloat() / 1024f)

        var status = ""
        if(m.isDownloading && m.isPaused) status = " (Paused)"
        else if(!m.isDownloading) {
            if(m.downloadProgress >= 1f)
                status = " (Finished)"
            else
                status = " (Stopped)"
        }

        var downloadedMB = m.torrents[0].size_bytes.toDouble() * m.downloadProgress.toDouble()
        downloadedMB = downloadedMB / 1024f / 1024f

        tvProgress.text = String.format(Locale.getDefault(),
                "%.2f%% (%.1f MB)%s", m.downloadProgress * 100f, downloadedMB, status)

        tvSeeds.text = String.format(Locale.getDefault(), "%d Seeds", m.downloadSeeds)

        if(m.isDownloading) {
            if(m.isPaused) {
                ivPause.setImageResource(R.drawable.ic_play_arrow_black_40dp)
            } else {
                ivPause.setImageResource(R.drawable.ic_pause_black_30dp)
            }
            ivDelete.setImageResource(R.drawable.ic_delete_grey_30dp)
            ivPause.visibility = View.VISIBLE
            ivStop.visibility = View.VISIBLE
        } else {
            ivPause.setImageResource(R.drawable.ic_file_download_black_40dp)
            ivDelete.setImageResource(R.drawable.ic_delete_black_30dp)
            ivStop.visibility = View.GONE
            if(m.downloadProgress >= 1f) {
                ivPause.visibility = View.GONE // download complete
            }
        }
    }

    override fun onImageLoaded(url: String, image: Bitmap) {
        if(url == loadingImageURL) {
            ivCover.setImageBitmap(image)
        }
    }

    fun registerReceiver() {
        val mgr = LocalBroadcastManager.getInstance(view.context)
        val filter = IntentFilter(DownloaderService.ACTION_DOWNLOAD_FAILED)
        filter.addAction(DownloaderService.ACTION_PROGRESS_UPDATE)
        filter.addAction(DownloaderService.ACTION_DOWNLOAD_COMPLETE)
        filter.addAction(DownloaderService.ACTION_DOWNLOAD_PAUSED)
        filter.addAction(DownloaderService.ACTION_DOWNLOAD_RESUMED)
        filter.addAction(DownloaderService.ACTION_DOWNLOAD_STOPPED)
        mgr.registerReceiver(downloadReceiver, filter)
    }

    fun unregisterReceiver() {
        saveProgress()
        val mgr = LocalBroadcastManager.getInstance(view.context)
        mgr.unregisterReceiver(downloadReceiver)
    }

    private fun saveProgress() {
        movie?.let {
            it.downloadSeeds = 0
            it.downloadSpeed = 0
            it.uploadSpeed = 0
            DownloadsPresenter.getInstance().updateDownloadProgress(it)
        }
    }

    private val downloadReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val mv = it.getSerializableExtra(DownloaderService.EXTRA_MOVIE) as Movie?
                mv?.let { m -> if(m.id == movie?.id) updateProgress(m) }
            }
        }

    }
}