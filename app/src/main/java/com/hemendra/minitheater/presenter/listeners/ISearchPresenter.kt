package com.hemendra.minitheater.presenter.listeners

interface ISearchPresenter {

    fun performSearch(query: String, pageNumber: Int, sortBy: String, genre: String)
    fun isSearching() : Boolean
    fun abort()
    fun destroy()

}