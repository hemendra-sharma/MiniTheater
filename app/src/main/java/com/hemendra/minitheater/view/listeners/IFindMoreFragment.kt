package com.hemendra.minitheater.view.listeners

import android.content.Context
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.model.movies.MoviesDataSourceFailureReason

interface IFindMoreFragment {
    fun getCtx(): Context?
    fun onSearchStarted(message: String)
    fun onSearchResults(results: ArrayList<Movie>)
    fun onMagnetURL(magnetURL: String)
    fun onSearchFailed(reason: MoviesDataSourceFailureReason)
}