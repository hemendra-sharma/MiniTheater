package com.hemendra.minitheater.view.more

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.view.listeners.OnMovieItemClickListener
import java.util.*

class ExtraMoviesListAdapter(private val movies: ArrayList<Movie>,
                             private val listener: OnMovieItemClickListener):
        RecyclerView.Adapter<ExtraMovieViewHolder>() {

    private var endReached = false

    fun appendData(movies: ArrayList<Movie>) {
        this.movies.addAll(movies)
        notifyDataSetChanged()
    }

    fun endReached() {
        endReached = true
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(vg: ViewGroup, position: Int): ExtraMovieViewHolder {
        val view = LayoutInflater.from(vg.context)
                .inflate(R.layout.extra_movie_list_item, vg, false)
        return ExtraMovieViewHolder(view, listener)
    }

    override fun getItemCount(): Int =
            if(endReached) movies.size
            else movies.size + 1

    override fun onBindViewHolder(holder: ExtraMovieViewHolder, i: Int) {
        if(i < movies.size) {
            holder.movie = movies[i]
            holder.fillDetails(movies[i])
        } else  holder.showProgress()
    }
}