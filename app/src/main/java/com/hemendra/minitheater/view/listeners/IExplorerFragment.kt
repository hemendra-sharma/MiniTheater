package com.hemendra.minitheater.view.listeners

import android.content.Context
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.model.movies.MoviesDataSourceFailureReason

interface IExplorerFragment {

    fun getCtx(): Context?
    fun onSearchStarted(message: String)
    fun onSearchResults(movies: ArrayList<Movie>)
    fun onSearchFailed(reason: MoviesDataSourceFailureReason)

}