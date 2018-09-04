package com.hemendra.minitheater.view.listeners

import android.graphics.Bitmap

interface ImageLoaderCallback {

    fun onImageLoaded(url: String, image: Bitmap)

}