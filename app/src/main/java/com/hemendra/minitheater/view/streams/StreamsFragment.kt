package com.hemendra.minitheater.view.streams

import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.presenter.ImagesPresenter
import com.hemendra.minitheater.presenter.StreamsPresenter
import com.hemendra.minitheater.view.listeners.OnMovieDownloadClickListener
import com.hemendra.minitheater.view.listeners.OnStreamItemClickListener
import com.hemendra.minitheater.view.listeners.StreamSearchListener
import kotlinx.android.synthetic.main.fragment_downloader.*

class StreamsFragment: Fragment(), StreamSearchListener {

    private val streamsPresenter = StreamsPresenter(this)

    private var savedView: View? = null
    private var adapter: StreamsListAdapter? = null

    private var handler: Handler? = null

    private var onStreamItemClickListener: OnStreamItemClickListener? = null
    fun setMovieClickListener(onStreamItemClickListener: OnStreamItemClickListener) {
        this.onStreamItemClickListener = onStreamItemClickListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        savedView?.let { return it }
        savedView = inflater.inflate(R.layout.fragment_downloader, container, false)
        return savedView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadFreshList()
        handler = Handler()
        handler?.postDelayed({
            tvNoDownloads?.text = Html.fromHtml("No Streams Found!<br><br>Tap Here to Retry")
            tvNoDownloads?.setOnClickListener{ loadFreshList() }
        }, 30000)
    }

    override fun onDestroyView() {
        context?.let { ImagesPresenter.getInstance(it).abortAll() }
        (savedView?.parent as ViewGroup?)?.removeAllViews()
        handler?.removeCallbacks(null)
        super.onDestroyView()
    }

    fun destroy() {
        savedView = null
        handler?.removeCallbacks(null)
        streamsPresenter.abort()
    }

    fun loadFreshList() {
        if(streamsPresenter.startSearch()) {
            adapter?.movies?.clear()
            adapter?.notifyDataSetChanged()
            tvNoDownloads.text = Html.fromHtml("Finding Streams...<br><br>" +
                    "Make Sure the Streaming Device is Also on the Same Wifi Network")
            tvNoDownloads.setOnClickListener(null)
            tvNoDownloads.visibility = View.VISIBLE
        } else if(adapter?.itemCount ?: 0 > 0) {
            tvNoDownloads.visibility = View.GONE
        }
    }

    override fun onMovieFound(movie: Movie) {
        tvNoDownloads.visibility = View.GONE
        adapter?.let {
            it.movies.add(movie)
            it.notifyDataSetChanged()
            return
        }
        onStreamItemClickListener?.let {
            adapter = StreamsListAdapter(it)
            adapter?.movies?.add(movie)
            recycler?.adapter = adapter
        }
    }
}