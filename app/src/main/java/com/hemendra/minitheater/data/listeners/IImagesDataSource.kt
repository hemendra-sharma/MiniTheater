package com.hemendra.minitheater.data.listeners

import com.hemendra.minitheater.view.listeners.ImageLoaderCallback

interface IImagesDataSource {

    fun loadImage(url: String, callback: ImageLoaderCallback)
    fun close()

}