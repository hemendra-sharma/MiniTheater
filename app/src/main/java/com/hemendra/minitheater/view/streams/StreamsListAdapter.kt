package com.hemendra.minitheater.view.streams

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.presenter.ImagesPresenter
import com.hemendra.minitheater.view.listeners.OnStreamItemClickListener
import java.util.ArrayList

class StreamsListAdapter(private val listener: OnStreamItemClickListener):
        RecyclerView.Adapter<StreamsViewHolder>() {

    var movies: ArrayList<Movie> = ArrayList()

    override fun onCreateViewHolder(vg: ViewGroup, position: Int): StreamsViewHolder {
        val view = LayoutInflater.from(vg.context)
                .inflate(R.layout.movie_list_item, vg, false)
        return StreamsViewHolder(view, listener, ImagesPresenter.getInstance(vg.context))
    }

    override fun getItemCount(): Int = movies.size

    override fun onBindViewHolder(holder: StreamsViewHolder, i: Int) {
        holder.fillDetails(movies[i])
    }

}