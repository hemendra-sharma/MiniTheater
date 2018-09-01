package com.hemendra.minitheater.model.movies

interface IMoviesDataSource {

    fun searchMovies(query: String, pageNumber: Int)
    fun abort()

}