package com.hemendra.minitheater.data.model.movies

import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.listeners.IMoviesDataSource
import com.hemendra.minitheater.presenter.listeners.IMoviesDataSourceListener
import com.hemendra.minitheater.data.listeners.OnMoviesLoadedListener

class MoviesDataSource(private var listener: IMoviesDataSourceListener):
        IMoviesDataSource, OnMoviesLoadedListener {

    private var moviesLoader: MoviesLoader? = null

    override fun searchMovies(query: String, pageNumber: Int) {
        moviesLoader?.let {
            listener.onFailure(MoviesDataSourceFailureReason.ALREADY_LOADING)
            return
        }
        moviesLoader = MoviesLoader(this)
        moviesLoader?.execute(query, pageNumber)
                ?: listener.onFailure(MoviesDataSourceFailureReason.UNKNOWN)
    }

    override fun isSearching(): Boolean  = moviesLoader != null

    override fun abort() : Boolean {
        moviesLoader?.let {
            it.cancel(true)
            moviesLoader = null
            return true
        }
        return false
    }

    override fun onMoviesLoaded(results: ArrayList<Movie>) {
        moviesLoader = null
        listener.onResult(results)
    }

    override fun onFailedToLoadMovies(reason: MoviesDataSourceFailureReason) {
        moviesLoader = null
        listener.onFailure(reason)
    }

}