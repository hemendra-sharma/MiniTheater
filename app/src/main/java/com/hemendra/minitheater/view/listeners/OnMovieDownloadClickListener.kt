package com.hemendra.minitheater.view.listeners

import com.hemendra.minitheater.data.Movie

interface OnMovieDownloadClickListener {

    fun onDownloadClicked(movie: Movie)
    fun onPlayClicked(movie: Movie)

}