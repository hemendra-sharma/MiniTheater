package com.hemendra.minitheater.presenter.search

interface ISearchPresenter {

    fun loadLandingPage()
    fun performSearch(query: String)
    fun abort()

}