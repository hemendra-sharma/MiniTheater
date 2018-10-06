package com.hemendra.minitheater.presenter

import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.model.streams.NetworkStreamSearcher
import com.hemendra.minitheater.view.listeners.StreamSearchListener

class StreamsPresenter(val listener: StreamSearchListener): StreamSearchListener {

    private var searcher: NetworkStreamSearcher? = null

    companion object {
        private var instance : StreamsPresenter? = null
        fun getInstance(listener: StreamSearchListener) : StreamsPresenter {
            if(instance == null) instance = StreamsPresenter(listener)
            return instance!!
        }
    }

    fun startSearch(): Boolean {
        if(searcher?.isExecuting() == true) return false

        searcher = NetworkStreamSearcher(this)
        searcher?.execute()
        return true
    }

    fun abort() {
        searcher?.cancel(true)
        searcher = null
    }

    override fun onMovieFound(movie: Movie) {
        listener.onMovieFound(movie)
    }
}