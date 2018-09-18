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
import android.widget.AdapterView
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.model.movies.MoviesDataSourceFailureReason
import com.hemendra.minitheater.presenter.ImagesPresenter
import com.hemendra.minitheater.presenter.listeners.ISearchPresenter
import com.hemendra.minitheater.presenter.SearchPresenter
import com.hemendra.minitheater.view.listeners.IExplorerFragment
import com.hemendra.minitheater.view.listeners.OnMovieItemClickListener
import kotlinx.android.synthetic.main.fragment_explorer.*
import android.widget.ArrayAdapter
import com.hemendra.minitheater.utils.RemoteConfig
import java.util.*


class ExplorerFragment: Fragment(), IExplorerFragment {

    private val searchPresenter: ISearchPresenter = SearchPresenter(this)
    private var lastSearched = ""
    private var lastPageNumber = 1
    private var adapter: MoviesListAdapter? = null
    lateinit var onMovieItemClickListener: OnMovieItemClickListener

    private var genreList: ArrayList<String>? = null
    private var sortingOptions: HashMap<String,String>? = null
    private var sortingOptionsKeys: ArrayList<String>? = null

    private var savedView: View? = null
    private var loadingFirstTime = true

    private var spinnerCallCheck = 0

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
        //val settingsItem = menu.findItem(R.id.action_settings)
        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity?.componentName))

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String): Boolean {
                searchView.clearFocus()
                lastSearched = query
                lastPageNumber = 1
                performSearch(query, lastPageNumber)
                return false
            }

            override fun onQueryTextChange(query: String): Boolean  = false
        })

        searchMenuItem.setOnActionExpandListener(object: MenuItem.OnActionExpandListener{
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean = true
            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                if(lastSearched.isNotEmpty()) {
                    lastSearched = ""
                    lastPageNumber = 1
                    performSearch("", lastPageNumber)
                }
                return true
            }

        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        savedView?.let { return it }
        savedView = inflater.inflate(R.layout.fragment_explorer, container, false)
        return savedView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar?.let { (activity as AppCompatActivity).setSupportActionBar(it) }

        context?.let {
            spinnerCallCheck = 0

            genreList = RemoteConfig.getInstance().getGenreOptions()
            sortingOptions = RemoteConfig.getInstance().getSortingOptionsMap()
            sortingOptionsKeys = RemoteConfig.getInstance().getSortingOptionsList()

            val genreSpinnerAdapter = ArrayAdapter<String>(it,
                    android.R.layout.simple_spinner_item, genreList!!)
            genreSpinnerAdapter.setDropDownViewResource(android.R.layout
                    .simple_spinner_dropdown_item)
            spinnerGenre?.adapter = genreSpinnerAdapter
            spinnerGenre?.onItemSelectedListener = onSpinnerItemSelected

            val sortingSpinnerAdapter = ArrayAdapter<String>(it,
                    android.R.layout.simple_spinner_item, sortingOptionsKeys!!)
            sortingSpinnerAdapter.setDropDownViewResource(android.R.layout
                    .simple_spinner_dropdown_item)
            spinnerSortBy?.adapter = sortingSpinnerAdapter
            spinnerSortBy?.onItemSelectedListener = onSpinnerItemSelected
        } ?: return

        recycler?.let {
            it.addOnScrollListener(object :
                    ContinuousScrollListener(it.layoutManager as LinearLayoutManager) {
                override fun onLoadMore() {
                    if (!searchPresenter.isSearching()) {
                        lastPageNumber++
                        performSearch(lastSearched, lastPageNumber)
                    }
                }
            })
        }

        tvProgress?.setOnClickListener { performSearch(lastSearched, lastPageNumber) }

        if(loadingFirstTime) {
            loadingFirstTime = false
            performSearch(lastSearched, lastPageNumber)
        }
    }

    override fun onDestroyView() {
        context?.let { ImagesPresenter.getInstance(it).abortAll() }
        (savedView?.parent as ViewGroup?)?.removeAllViews()
        spinnerGenre?.onItemSelectedListener = null
        spinnerSortBy?.onItemSelectedListener = null
        super.onDestroyView()
    }

    private val onSpinnerItemSelected = object: AdapterView.OnItemSelectedListener{
        override fun onItemSelected(parent: AdapterView<*>?,
                                    view: View?, position: Int, id: Long) {
            if(++spinnerCallCheck > 2) {
                lastPageNumber = 1
                performSearch(lastSearched, lastPageNumber)
            }
        }
        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }

    private fun performSearch(query: String, pageNumber: Int) {
        var sortBy = ""
        var genre = ""
        sortingOptions?.let { map ->
            if(map.size > 0) {
                spinnerSortBy?.let {
                    if (it.selectedItemPosition != AdapterView.INVALID_POSITION) {
                        sortingOptionsKeys?.let { keys ->
                            sortBy = sortingOptions?.get(keys[it.selectedItemPosition]) ?: ""
                        }
                    }
                }
            }
        }
        genreList?.let { list ->
            spinnerGenre?.let {
                if (spinnerGenre.selectedItemPosition != AdapterView.INVALID_POSITION) {
                    genre = list[it.selectedItemPosition]
                }
            }
        }
        searchPresenter.performSearch(query, pageNumber, sortBy, genre)
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
            recycler?.adapter = adapter
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
        val mLayoutManager = recycler?.layoutManager as LinearLayoutManager?
        val visibleItemCount = mLayoutManager?.childCount ?: 0
        val totalItemCount = mLayoutManager?.itemCount ?: 0
        val pastVisibleItemsCount = mLayoutManager?.findFirstVisibleItemPosition() ?: -1

        if ((visibleItemCount + pastVisibleItemsCount) >= totalItemCount) {
            lastPageNumber = 2
            performSearch(lastSearched, lastPageNumber)
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
        return rlProgress ?. let { it.visibility == View.VISIBLE } ?: true
    }

    private fun showProgress(msg: String) {
        pbProgress?.visibility = View.VISIBLE
        tvProgress?.visibility = View.VISIBLE
        rlProgress?.visibility = View.VISIBLE
        tvProgress?.text = msg
    }

    private fun hideProgress() {
        rlProgress?.let { it.visibility = View.GONE }
    }

    private fun showError(error: String) {
        rlProgress?.visibility = View.VISIBLE
        tvProgress?.visibility = View.VISIBLE
        pbProgress?.visibility = View.INVISIBLE
        tvProgress?.text = String.format(Locale.getDefault(), "%s\n\nTap Here to Retry!", error)
    }
}