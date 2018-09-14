package com.hemendra.minitheater.data.listeners

interface IMoviesDataSource {

    fun searchMovies(query: String, pageNumber: Int, sortBy: String, genre: String)
    fun isSearching() : Boolean
    fun abort() : Boolean

}