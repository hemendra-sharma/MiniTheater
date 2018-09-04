package com.hemendra.minitheater.data.listeners

interface IMoviesDataSource {

    fun searchMovies(query: String, pageNumber: Int)
    fun isSearching() : Boolean
    fun abort() : Boolean

}