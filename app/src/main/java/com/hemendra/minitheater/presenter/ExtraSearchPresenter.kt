package com.hemendra.minitheater.presenter

import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.listeners.IExtraMoviesDataSource
import com.hemendra.minitheater.data.model.movies.ExtraMoviesDataSource
import com.hemendra.minitheater.data.model.movies.MoviesDataSourceFailureReason
import com.hemendra.minitheater.presenter.listeners.IExtraSearchPresenter
import com.hemendra.minitheater.presenter.listeners.IMoviesDataSourceListener
import com.hemendra.minitheater.utils.Utils
import com.hemendra.minitheater.view.listeners.IFindMoreFragment

class ExtraSearchPresenter(private var explorer: IFindMoreFragment):
        IExtraSearchPresenter, IMoviesDataSourceListener {

    private var destroyed = false
    private val moviesDataSource: IExtraMoviesDataSource = ExtraMoviesDataSource(this)

    override fun performSearch(query: String, pageNumber: Int) {
        abort() // abort any previous ongoing process
        if(destroyed) return

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

    override fun getMagnetURL(pageURL: String) {
        abort() // abort any previous ongoing process
        if(destroyed) return

        val ctx = explorer.getCtx()
        if(ctx != null && !Utils.isNetworkAvailable(ctx)) {
            explorer.onSearchFailed(MoviesDataSourceFailureReason.NO_INTERNET_CONNECTION)
            return
        }
        moviesDataSource.getMagnetURL(pageURL)
        explorer.onSearchStarted("Getting Movie Info")
    }

    override fun isSearching() : Boolean = !destroyed && moviesDataSource.isSearching()

    override fun abort() {
        if(!destroyed && moviesDataSource.abort())
            explorer.onSearchFailed(MoviesDataSourceFailureReason.ABORTED)
    }

    override fun destroy() {
        abort()
        destroyed = true
    }

    override fun onResult(results: ArrayList<Movie>) {
        if(!destroyed) explorer.onSearchResults(results)
    }

    override fun onMagnetURL(magnetURL: String) {
        if(!destroyed) explorer.onMagnetURL(magnetURL)
    }

    override fun onFailure(reason: MoviesDataSourceFailureReason) {
        if(!destroyed) explorer.onSearchFailed(reason)
    }
}