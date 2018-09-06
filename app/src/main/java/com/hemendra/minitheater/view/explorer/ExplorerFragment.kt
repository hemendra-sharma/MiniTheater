package com.hemendra.minitheater.view.explorer

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.*
import android.widget.Toast
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.model.movies.MoviesDataSourceFailureReason
import com.hemendra.minitheater.presenter.ImagesPresenter
import com.hemendra.minitheater.presenter.listeners.ISearchPresenter
import com.hemendra.minitheater.presenter.SearchPresenter
import com.hemendra.minitheater.view.listeners.IExplorerFragment
import com.hemendra.minitheater.view.listeners.OnMovieItemClickListener
import kotlinx.android.synthetic.main.fragment_explorer.*

class ExplorerFragment: Fragment(), IExplorerFragment {

    private val searchPresenter: ISearchPresenter = SearchPresenter(this)
    private var lastSearched = ""
    private var lastPageNumber = 1
    private var adapter: MoviesListAdapter? = null
    lateinit var onMovieItemClickListener: OnMovieItemClickListener

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
        val searchMenuItem = menu.findItem(R.id.action_search)
        val searchView: SearchView = searchMenuItem.actionView as SearchView
        val settingsItem = menu.findItem(R.id.action_settings)
        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity?.componentName))

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String): Boolean {
                searchView.clearFocus()
                lastSearched = query
                lastPageNumber = 1
                searchPresenter.performSearch(query, lastPageNumber)
                return false
            }

            override fun onQueryTextChange(query: String): Boolean  = false
        })

        searchMenuItem.setOnActionExpandListener(object: MenuItem.OnActionExpandListener{
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean = true
            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                lastSearched = ""
                lastPageNumber = 1
                searchPresenter.performSearch("", lastPageNumber)
                return true
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
        lastSearched = ""
        lastPageNumber = 1
        recycler.addOnScrollListener(object :
                ContinuousScrollListener(recycler.layoutManager as LinearLayoutManager) {
            override fun onLoadMore() {
                if(!searchPresenter.isSearching()) {
                    lastPageNumber++
                    searchPresenter.performSearch(lastSearched, lastPageNumber)
                }
            }
        })
        searchPresenter.performSearch(lastSearched, lastPageNumber)
    }

    override fun onDestroyView() {
        searchPresenter.destroy()
        context?.let { ImagesPresenter.getInstance(it).abortAll() }
        super.onDestroyView()
    }

    override fun getCtx(): Context? = context

    override fun onSearchStarted(message: String) {
        if(lastPageNumber == 1)
            showProgress(message)
    }

    override fun onSearchResults(movies: ArrayList<Movie>) {
        hideProgress()
        if(lastPageNumber == 1) {
            adapter = MoviesListAdapter(movies, onMovieItemClickListener)
            recycler.adapter = adapter
            Handler().postDelayed({ checkLoadMore() }, 1000)
        } else {
            adapter?.appendData(movies)
        }
    }

    override fun onSearchFailed(reason: MoviesDataSourceFailureReason) {
        when(reason) {
            MoviesDataSourceFailureReason.ABORTED -> { /** ignore **/ }
            MoviesDataSourceFailureReason.NETWORK_TIMEOUT -> showError("Network Timeout!")
            MoviesDataSourceFailureReason.NO_INTERNET_CONNECTION -> showError("No Internet Connection!")
            MoviesDataSourceFailureReason.NO_SEARCH_RESULTS -> {
                adapter?.endReached()
                if(lastPageNumber == 1) showError("No Search Results!")
            }
            MoviesDataSourceFailureReason.ALREADY_LOADING -> {
                if(lastPageNumber > 1) lastPageNumber--
            }
            MoviesDataSourceFailureReason.API_MISSING -> showError("Something Wrong on Server!")
            MoviesDataSourceFailureReason.UNKNOWN -> showError("Unknown Error!")
        }
    }

    private fun checkLoadMore() {
        val mLayoutManager = recycler.layoutManager as LinearLayoutManager
        val visibleItemCount = mLayoutManager.childCount
        val totalItemCount = mLayoutManager.itemCount
        val pastVisibleItemsCount = mLayoutManager.findFirstVisibleItemPosition()

        if ((visibleItemCount + pastVisibleItemsCount) >= totalItemCount) {
            lastPageNumber = 2
            searchPresenter.performSearch(lastSearched, lastPageNumber)
        }
    }

    fun onBackPressed(): Boolean {
        return if(isProgressOrErrorVisible()) {
            searchPresenter.abort()
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