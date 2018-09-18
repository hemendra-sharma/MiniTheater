package com.hemendra.minitheater.presenter.listeners

interface IExtraSearchPresenter {
    fun performSearch(query: String, pageNumber: Int)
    fun getMagnetURL(pageURL: String)
    fun isSearching() : Boolean
    fun abort()
    fun destroy()
}