package com.hemendra.minitheater.view.explorer

import com.hemendra.minitheater.data.Movie

interface IExplorerFragment {

    fun onSearchStarted(message: String)
    fun onSearchResults(movies: ArrayList<Movie>)
    fun onSearchFailed(reason: ExplorerFailureReason)

}