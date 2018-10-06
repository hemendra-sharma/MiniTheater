package com.hemendra.minitheater.view.listeners

import com.hemendra.minitheater.data.Movie

@FunctionalInterface
interface OnStreamItemClickListener {
    fun onItemClick(movie: Movie)
}