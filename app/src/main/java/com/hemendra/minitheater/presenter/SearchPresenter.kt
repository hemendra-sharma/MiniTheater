package com.hemendra.minitheater.presenter

import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.listeners.IMoviesDataSource
import com.hemendra.minitheater.presenter.listeners.IMoviesDataSourceListener
import com.hemendra.minitheater.data.model.movies.MoviesDataSource
import com.hemendra.minitheater.data.model.movies.MoviesDataSourceFailureReason
import com.hemendra.minitheater.presenter.listeners.ISearchPresenter
import com.hemendra.minitheater.utils.Utils
import com.hemendra.minitheater.view.listeners.IExplorerFragment

class SearchPresenter(private var explorer: IExplorerFragment):
        ISearchPresenter, IMoviesDataSourceListener {

    private val moviesDataSource: IMoviesDataSource = MoviesDataSource(this)

    override fun performSearch(query: String, pageNumber: Int) {
        abort() // abort any previous ongoing process
        val ctx = explorer.getCtx()
        if(ctx != null && !Utils.isNetworkAvailable(ctx)) {
            explorer.onSearchFailed(MoviesDataSourceFailureReason.NO_INTERNET_CONNECTION)
            return
        }
        moviesDataSource.searchMovies(query, pageNumber)
        if(query.isEmpty())
            explorer.onSearchStarted("")
        else
            explorer.onSearchStarted("Searching '$query'")
    }

    override fun isSearching() : Boolean = moviesDataSource.isSearching()

    override fun abort() {
        if(moviesDataSource.abort()) explorer.onSearchFailed(MoviesDataSourceFailureReason.ABORTED)
    }

    override fun onResult(results: ArrayList<Movie>) {
        explorer.onSearchResults(results)
    }

    override fun onFailure(reason: MoviesDataSourceFailureReason) {
        explorer.onSearchFailed(reason)
    }
}