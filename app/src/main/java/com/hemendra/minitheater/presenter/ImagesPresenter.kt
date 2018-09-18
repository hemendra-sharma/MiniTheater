package com.hemendra.minitheater.presenter

import android.content.Context
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.model.images.ImagesDataSource
import com.hemendra.minitheater.presenter.listeners.IImagesPresenter
import com.hemendra.minitheater.utils.RemoteConfig
import com.hemendra.minitheater.view.listeners.ImageLoaderCallback

class ImagesPresenter private constructor(context: Context) : IImagesPresenter {

    companion object {
        private var instance : ImagesPresenter? = null
        fun getInstance(context: Context) : ImagesPresenter {
            if(instance == null) instance = ImagesPresenter(context)
            return instance!!
        }
    }

    private val imagesDataSource = ImagesDataSource(context)

    override fun loadCoverImage(movie: Movie, callback: ImageLoaderCallback) : String {
        if(movie.small_cover_image.isNotEmpty()) {
            val url = RemoteConfig.getInstance().getConvertedImageURL(movie.medium_cover_image)
            imagesDataSource.loadImage(url, callback)
            return url
        }
        return ""
    }

    override fun abortAll() {
        imagesDataSource.abortAll()
    }

    override fun close() {
        imagesDataSource.abortAll()
        imagesDataSource.close()
        instance = null
    }

}