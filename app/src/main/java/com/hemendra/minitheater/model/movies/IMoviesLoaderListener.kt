package com.hemendra.minitheater.model.movies

import com.hemendra.minitheater.data.Movie

interface IMoviesLoaderListener {

    fun onMoviesLoaded(results: ArrayList<Movie>)
    fun onFailedToLoadMovies(reason: MoviesDataSourceFailureReason)

}