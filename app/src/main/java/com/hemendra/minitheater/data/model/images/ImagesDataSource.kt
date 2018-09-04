package com.hemendra.minitheater.data.model.images

import android.content.Context
import com.hemendra.minitheater.data.listeners.IImageLoaderListener
import com.hemendra.minitheater.data.listeners.IImagesDataSource
import com.hemendra.minitheater.view.listeners.ImageLoaderCallback

class ImagesDataSource(context: Context) : IImagesDataSource, IImageLoaderListener {

    private val db = ImagesDB(context)
    private val downloadPool = DownloadPool()

    override fun loadImage(url: String, callback: ImageLoaderCallback) {
        downloadPool.newDownload(ImageLoader(db, url, callback, this))
    }

    override fun close() {
        downloadPool.abortAllDownloads()
        db.close()
    }

    override fun onExecutionFinished() {
        downloadPool.restackElementFromQueue()
    }

}