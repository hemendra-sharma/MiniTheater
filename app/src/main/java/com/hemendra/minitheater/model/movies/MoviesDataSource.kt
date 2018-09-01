package com.hemendra.minitheater.model.movies

import com.hemendra.minitheater.data.Movie

class MoviesDataSource(private var listener: IMoviesDataSourceListener):
        IMoviesDataSource, IMoviesLoaderListener {

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

    override fun abort() {
        moviesLoader?.let {
            it.stop(true)
            moviesLoader = null
        }
    }

    override fun onMoviesLoaded(results: ArrayList<Movie>) {
        listener.onResult(results)
    }

    override fun onFailedToLoadMovies(reason: MoviesDataSourceFailureReason) {
        listener.onFailure(reason)
    }

}