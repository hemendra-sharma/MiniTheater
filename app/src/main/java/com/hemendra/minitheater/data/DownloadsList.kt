package com.hemendra.minitheater.data

import java.io.Serializable

class DownloadsList: Serializable {

    var movies: ArrayList<Movie> = ArrayList()

    fun add(movie: Movie): Boolean {
        return if(!movies.contains(movie)) { movies.add(movie); true }
        else false
    }

    fun remove(movie: Movie): Boolean {
        return if(!movies.contains(movie)) { movies.remove(movie); true }
        else false
    }

}