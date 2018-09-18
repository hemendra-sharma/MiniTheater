package com.hemendra.minitheater.data.model.movies

import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.listeners.IExtraMoviesDataSource
import com.hemendra.minitheater.data.listeners.OnMoviesLoadedListener
import com.hemendra.minitheater.presenter.listeners.IMoviesDataSourceListener

class ExtraMoviesDataSource(private val listener: IMoviesDataSourceListener):
    IExtraMoviesDataSource, OnMoviesLoadedListener {

    private var moviesLoader: ExtraMoviesLoader? = null
    private var extraMovieMagnetUrlLoader: ExtraMovieMagnetUrlLoader? = null

    override fun searchMovies(query: String, pageNumber: Int) {
        moviesLoader?.let {
            listener.onFailure(MoviesDataSourceFailureReason.ALREADY_LOADING)
            return
        }
        moviesLoader = ExtraMoviesLoader(this)
        moviesLoader?.execute(query, pageNumber)
                ?: listener.onFailure(MoviesDataSourceFailureReason.UNKNOWN)
    }

    override fun getMagnetURL(pageURL: String) {
        extraMovieMagnetUrlLoader?.let {
            listener.onFailure(MoviesDataSourceFailureReason.ALREADY_LOADING)
            return
        }
        extraMovieMagnetUrlLoader = ExtraMovieMagnetUrlLoader(this)
        extraMovieMagnetUrlLoader?.execute(pageURL)
                ?: listener.onFailure(MoviesDataSourceFailureReason.UNKNOWN)
    }

    override fun isSearching(): Boolean  = moviesLoader != null

    override fun abort() : Boolean {
        moviesLoader?.let {
            it.cancel(true)
            moviesLoader = null
            return true
        }
        extraMovieMagnetUrlLoader?.let {
            it.cancel(true)
            extraMovieMagnetUrlLoader = null
            return true
        }
        return false
    }

    override fun onMoviesLoaded(results: ArrayList<Movie>) {
        moviesLoader = null
        listener.onResult(results)
    }

    override fun onMagnetURL(magnetURL: String) {
        extraMovieMagnetUrlLoader = null
        listener.onMagnetURL(magnetURL)
    }

    override fun onFailedToLoadMovies(reason: MoviesDataSourceFailureReason) {
        moviesLoader = null
        extraMovieMagnetUrlLoader = null
        listener.onFailure(reason)
    }
}