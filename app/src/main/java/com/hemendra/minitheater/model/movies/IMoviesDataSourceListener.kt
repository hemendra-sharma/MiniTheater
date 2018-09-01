package com.hemendra.minitheater.model.movies

import com.hemendra.minitheater.data.Movie

interface IMoviesDataSourceListener {

    fun onResult(results: ArrayList<Movie>)
    fun onFailure(reason: MoviesDataSourceFailureReason)

}