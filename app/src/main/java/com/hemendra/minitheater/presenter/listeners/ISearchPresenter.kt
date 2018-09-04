package com.hemendra.minitheater.presenter.listeners

interface ISearchPresenter {

    fun performSearch(query: String, pageNumber: Int)
    fun isSearching() : Boolean
    fun abort()

}