package com.hemendra.minitheater.presenter.search

import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.model.movies.IMoviesDataSource
import com.hemendra.minitheater.model.movies.IMoviesDataSourceListener
import com.hemendra.minitheater.model.movies.MoviesDataSource
import com.hemendra.minitheater.model.movies.MoviesDataSourceFailureReason
import com.hemendra.minitheater.view.explorer.IExplorerFragment

class SearchPresenter(private var explorer: IExplorerFragment): ISearchPresenter, IMoviesDataSourceListener {

    private val moviesDataSource: IMoviesDataSource = MoviesDataSource(this)

    override fun performSearch(query: String, pageNumber: Int) {
        moviesDataSource.searchMovies(query, pageNumber)
        if(query.isEmpty())
            explorer.onSearchStarted("Loading... Please Wait!")
        else
            explorer.onSearchStarted("Searching '$query'")
    }

    override fun abort() {
        moviesDataSource.abort()
        explorer.onSearchFailed(MoviesDataSourceFailureReason.ABORTED)
    }

    override fun onResult(results: ArrayList<Movie>) {
        explorer.onSearchResults(results)
    }

    override fun onFailure(reason: MoviesDataSourceFailureReason) {
        explorer.onSearchFailed(reason)
    }
}