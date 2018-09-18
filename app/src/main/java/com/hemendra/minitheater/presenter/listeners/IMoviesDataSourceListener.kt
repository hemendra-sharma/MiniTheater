package com.hemendra.minitheater.presenter.listeners

import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.model.movies.MoviesDataSourceFailureReason

interface IMoviesDataSourceListener {

    fun onResult(results: ArrayList<Movie>)
    fun onMagnetURL(magnetURL: String)
    fun onFailure(reason: MoviesDataSourceFailureReason)

}