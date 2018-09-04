package com.hemendra.minitheater.data.listeners

import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.model.movies.MoviesDataSourceFailureReason

interface OnMoviesLoadedListener {

    fun onMoviesLoaded(results: ArrayList<Movie>)
    fun onFailedToLoadMovies(reason: MoviesDataSourceFailureReason)

}