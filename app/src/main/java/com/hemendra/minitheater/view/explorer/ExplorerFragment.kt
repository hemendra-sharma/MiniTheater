package com.hemendra.minitheater.view.explorer

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.view.*
import android.widget.Toast
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.presenter.search.ISearchPresenter
import com.hemendra.minitheater.presenter.search.SearchPresenter
import kotlinx.android.synthetic.main.fragment_explorer.*

class ExplorerFragment: Fragment(), IExplorerFragment {

    private val searchPresenter: ISearchPresenter = SearchPresenter(this)

    companion object {
        var instance: ExplorerFragment = ExplorerFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu?, menuInflater: MenuInflater?) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater ?: return
        context ?: return
        activity ?: return
        menu ?: return

        menuInflater.inflate(R.menu.main, menu)

        // Associate searchable configuration with the SearchView
        val searchManager = context?.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView: SearchView = menu.findItem(R.id.action_search).actionView as SearchView
        val settingsItem = menu.findItem(R.id.action_settings)
        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity?.componentName))

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String): Boolean {
                searchView.clearFocus()
                searchPresenter.performSearch(query)
                return false
            }

            override fun onQueryTextChange(query: String): Boolean {
                return false
            }

        })

        searchView.setOnQueryTextFocusChangeListener {
            _, hasFocus -> settingsItem.isVisible = hasFocus }

        settingsItem.setOnMenuItemClickListener {
            Toast.makeText(activity, "1", Toast.LENGTH_SHORT).show()
            true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_explorer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        searchPresenter.loadLandingPage()
    }

    override fun onSearchStarted(message: String) {
        showProgress(message)
    }

    override fun onSearchResults(movies: ArrayList<Movie>) {
        // TODO("not implemented") // populate 'recycler' adapter and hide progress
        hideProgress()
        activity ?: Toast.makeText(activity, movies.size.toString(), Toast.LENGTH_SHORT).show()
    }

    override fun onSearchFailed(reason: ExplorerFailureReason) {
        when(reason) {
            ExplorerFailureReason.ABORTED -> showError("Search Aborted !")
            ExplorerFailureReason.NETWORK_TIMEOUT -> showError("Network Timeout!")
            ExplorerFailureReason.NO_INTERNET_CONNECTION -> showError("No Internet Connection!")
            ExplorerFailureReason.NO_SEARCH_RESULTS -> showError("No Search Results!")
        }
    }

    fun onBackPressed(): Boolean {
        return if(isProgressOrErrorVisible()) {
            hideProgress()
            true
        } else false
    }

    private fun isProgressOrErrorVisible(): Boolean {
        return rlProgress.visibility == View.VISIBLE
    }

    private fun showProgress(msg: String) {
        pbProgress.visibility = View.VISIBLE
        tvProgress.visibility = View.VISIBLE
        rlProgress.visibility = View.VISIBLE
        tvProgress.text = msg
    }

    private fun hideProgress() {
        rlProgress.visibility = View.GONE
    }

    private fun showError(error: String) {
        rlProgress.visibility = View.VISIBLE
        tvProgress.visibility = View.VISIBLE
        pbProgress.visibility = View.GONE
        tvProgress.text = error
    }
}