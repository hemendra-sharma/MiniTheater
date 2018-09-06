package com.hemendra.minitheater.data

import java.io.Serializable

class DownloadsList: Serializable {

    var movies: ArrayList<Movie> = ArrayList()

    fun add(movie: Movie): Boolean {
        for(m in movies) {
            if(m.id == movie.id)
                return false
        }
        movies.add(movie)
        return true
    }

    fun remove(movie: Movie): Boolean {
        for(i in 0 until movies.size) {
            if(movies[i].id == movie.id) {
                movies.removeAt(i)
                return true
            }
        }
        return false
    }

    fun update(movie: Movie): Boolean {
        for(i in 0 until movies.size) {
            if(movies[i].id == movie.id) {
                movies[i] = movie
                return true
            }
        }
        return false
    }

    fun startDownload(movie: Movie): Boolean {
        var updated = false
        for(m in movies) {
            if(m.id == movie.id) {
                m.isDownloading = true
                movie.isDownloading = true
                updated = true
            } else {
                m.isDownloading = false
            }
        }
        return updated
    }

    fun pauseDownload(movie: Movie): Boolean {
        var paused = false
        for(m in movies) {
            if(m.id == movie.id) {
                m.isDownloading = false
                movie.isDownloading = false
                paused = true
            }
        }
        return paused
    }

}