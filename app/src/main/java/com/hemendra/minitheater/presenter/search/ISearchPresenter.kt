package com.hemendra.minitheater.presenter.search

interface ISearchPresenter {

    fun performSearch(query: String, pageNumber: Int)
    fun abort()

}