package com.hemendra.minitheater.view.listeners

import com.hemendra.minitheater.data.Movie

interface OnDownloadItemClickListener {
    fun onItemClicked(movie: Movie)
    fun onPauseClicked(movie: Movie)
    fun onDeleteClicked(movie: Movie)
}