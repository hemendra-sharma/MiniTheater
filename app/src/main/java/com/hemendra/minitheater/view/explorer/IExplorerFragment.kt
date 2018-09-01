package com.hemendra.minitheater.view.explorer

import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.model.movies.MoviesDataSourceFailureReason

interface IExplorerFragment {

    fun onSearchStarted(message: String)
    fun onSearchResults(movies: ArrayList<Movie>)
    fun onSearchFailed(reason: MoviesDataSourceFailureReason)

}