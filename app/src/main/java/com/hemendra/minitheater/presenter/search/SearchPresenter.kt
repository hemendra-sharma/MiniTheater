package com.hemendra.minitheater.presenter.search

import android.os.Handler
import com.hemendra.minitheater.view.explorer.IExplorerFragment

class SearchPresenter(var explorer: IExplorerFragment): ISearchPresenter {

    override fun loadLandingPage() {
        // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        explorer.onSearchStarted("Loading... Please Wait!")
        Handler().postDelayed({ explorer.onSearchResults(ArrayList()) }, 1000)
    }

    override fun performSearch(query: String) {
        // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun abort() {
        // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}