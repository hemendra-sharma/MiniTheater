package com.hemendra.minitheater.data.listeners

interface IExtraMoviesDataSource {
    fun searchMovies(query: String, pageNumber: Int)
    fun getMagnetURL(pageURL: String)
    fun isSearching() : Boolean
    fun abort() : Boolean
}