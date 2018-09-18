package com.hemendra.minitheater.presenter.listeners

import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.view.listeners.ImageLoaderCallback

interface IImagesPresenter {

    fun loadCoverImage(movie: Movie, callback: ImageLoaderCallback) : String
    fun abortAll()
    fun close()

}