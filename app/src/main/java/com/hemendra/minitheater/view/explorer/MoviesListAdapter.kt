package com.hemendra.minitheater.view.explorer

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.presenter.ImagesPresenter
import com.hemendra.minitheater.view.listeners.OnMovieItemClickListener
import java.util.*

class MoviesListAdapter(private val movies: ArrayList<Movie>,
                        private val listener: OnMovieItemClickListener):
        RecyclerView.Adapter<MovieViewHolder>() {

    private var endReached = false

    fun appendData(movies: ArrayList<Movie>) {
        this.movies.addAll(movies)
        notifyDataSetChanged()
    }

    fun endReached() {
        endReached = true
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(vg: ViewGroup, position: Int): MovieViewHolder {
        val view = LayoutInflater.from(vg.context)
                .inflate(R.layout.movie_list_item, vg, false)
        return MovieViewHolder(view, listener, ImagesPresenter.getInstance(vg.context))
    }

    override fun getItemCount(): Int =
            if(endReached) movies.size
            else movies.size + 1

    override fun onBindViewHolder(holder: MovieViewHolder, i: Int) {
        if(i < movies.size) {
            holder.movie = movies[i]
            holder.fillDetails(movies[i])
        } else  holder.showProgress()
    }
}