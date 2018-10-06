package com.hemendra.minitheater.view.listeners

import com.hemendra.minitheater.data.Movie

interface StreamSearchListener {
    fun onMovieFound(movie: Movie)
}