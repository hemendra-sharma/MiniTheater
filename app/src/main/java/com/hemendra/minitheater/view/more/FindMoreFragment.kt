package com.hemendra.minitheater.view.more

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.Torrent
import com.hemendra.minitheater.data.model.movies.MoviesDataSourceFailureReason
import com.hemendra.minitheater.presenter.ExtraSearchPresenter
import com.hemendra.minitheater.presenter.listeners.IExtraSearchPresenter
import com.hemendra.minitheater.view.explorer.ContinuousScrollListener
import com.hemendra.minitheater.view.listeners.IFindMoreFragment
import com.hemendra.minitheater.view.listeners.OnMovieDownloadClickListener
import com.hemendra.minitheater.view.listeners.OnMovieItemClickListener
import com.hemendra.minitheater.view.showMessage
import com.hemendra.minitheater.view.showYesNoMessage
import kotlinx.android.synthetic.main.fragment_find_more.*
import java.util.*


class FindMoreFragment: Fragment(), IFindMoreFragment {

    private val searchPresenter: IExtraSearchPresenter = ExtraSearchPresenter(this)

    private var lastSearched = ""
    private var lastPageNumber = 1
    private var adapter: ExtraMoviesListAdapter? = null

    private var savedView: View? = null

    private var clickedMovie: Movie? = null

    private var onMovieDownloadClickListener: OnMovieDownloadClickListener? = null

    fun setMovieDownloadClickListener(listener: OnMovieDownloadClickListener) {
        this.onMovieDownloadClickListener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        savedView?.let { return it }
        savedView = inflater.inflate(R.layout.fragment_find_more, container, false)
        return savedView
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        etSearch?.setOnTouchListener(onSearchTouchListener)
        etSearch?.setOnEditorActionListener(onEditorActionListener)

        recycler?.let {
            recycler.addOnScrollListener(object :
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
    }

    private fun performSearch(query: String, pageNumber: Int) {
        if(query.trim().isNotEmpty()) {
            hideKeyboard()
            dockSearchViewToTop()
            recycler?.visibility = View.VISIBLE
            searchPresenter.performSearch(query, pageNumber)
        } else {
            context?.let { showMessage(it, "Enter Movie Name to Search") }
        }
    }

    override fun onDestroyView() {
        (savedView?.parent as ViewGroup?)?.removeAllViews()
        super.onDestroyView()
    }

    override fun getCtx(): Context? = context

    override fun onSearchStarted(message: String) {
        if(lastPageNumber == 1)
            showProgress(message)
    }

    private var onMovieItemClickListener = object : OnMovieItemClickListener {
        override fun onMovieItemClicked(movie: Movie) {
            if(movie.url.isNotEmpty()) {
                clickedMovie = movie
                searchPresenter.getMagnetURL(movie.url)
            } else context?.let {
                clickedMovie = null
                showMessage(it, "Movie Information unavailable !")
            }
        }
    }

    override fun onSearchResults(results: ArrayList<Movie>) {
        hideProgress()

        if(lastPageNumber == 1) {
            adapter = ExtraMoviesListAdapter(results, onMovieItemClickListener)
            recycler?.adapter = adapter
            Handler().postDelayed({ checkLoadMore() }, 1000)
        } else {
            adapter?.appendData(results)
        }
    }

    override fun onMagnetURL(magnetURL: String) {
        hideProgress()
        clickedMovie?.let { movie ->
            movie.id = magnetURL.hashCode()
            val torrent = Torrent()
            torrent.hash = getHashFromMagnetURL(magnetURL)
            torrent.url = magnetURL
            movie.torrents.add(torrent)
            context?.let { ctx ->
                showYesNoMessage(ctx, "This movie can be downloaded. " +
                        "Do you want to add it to your downloads list?", Runnable {
                    onMovieDownloadClickListener?.onDownloadClicked(movie)
                })
            }
        }
    }

    private fun getHashFromMagnetURL(url: String): String {
        val x = url.indexOf("btih:") + 5
        val y = url.indexOf("&", startIndex = x)
        return url.substring(x, y)
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

    private var touchDownOnDrawable = false
    private val onSearchTouchListener = View.OnTouchListener { _, event ->

        etSearch?.let { et ->
            if (event.action == MotionEvent.ACTION_DOWN
                    && event.rawX >= et.right - et.totalPaddingEnd) {
                touchDownOnDrawable = true
                true
            } else if (touchDownOnDrawable
                    && event.action == MotionEvent.ACTION_UP
                    && event.rawX >= et.right - et.totalPaddingEnd) {
                touchDownOnDrawable = false
                lastSearched = et.text.toString()
                lastPageNumber = 1
                performSearch(lastSearched, lastPageNumber)
                true
            } else false
        }
        false
    }

    private val onEditorActionListener = TextView.OnEditorActionListener { _, actionId, _ ->
        if(actionId == EditorInfo.IME_ACTION_SEARCH) {
            lastSearched = etSearch?.text?.toString() ?: ""
            lastPageNumber = 1
            performSearch(lastSearched, lastPageNumber)
            true
        } else false
    }

    private fun hideKeyboard() {
        context?.let {
            etSearch?.clearFocus()
            val mgr = it.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            mgr.hideSoftInputFromWindow(etSearch?.windowToken, 0)
        }
    }

    private var cardIsOnTop = false
    private fun dockSearchViewToTop() {
        if(!cardIsOnTop) {
            cardIsOnTop = true
            cardSearch?.let { card ->
                val params = card.layoutParams as RelativeLayout.LayoutParams
                val currentY = (resources.displayMetrics.heightPixels.toFloat() / 2f) -
                        card.height.toFloat() - (params.topMargin * 2f)
                val anim = TranslateAnimation(0f, 0f, 0f, -currentY)
                anim.duration = 500
                anim.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationRepeat(animation: Animation?) {}
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) {
                        params.removeRule(RelativeLayout.CENTER_IN_PARENT)
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
                        card.layoutParams = params
                    }
                })
                card.startAnimation(anim)
            }
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