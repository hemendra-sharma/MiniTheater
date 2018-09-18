package com.hemendra.minitheater.view.more

import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.view.listeners.OnMovieItemClickListener
import java.util.*

class ExtraMovieViewHolder(view: View, private val listener: OnMovieItemClickListener):
        RecyclerView.ViewHolder(view) {

    private val card: CardView = view as CardView
    private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
    private val tvSeeds: TextView = view.findViewById(R.id.tvSeeds)
    private val tvPeers: TextView = view.findViewById(R.id.tvPeers)
    private val pb: ProgressBar = view.findViewById(R.id.pb)
    var movie: Movie? = null

    init {
        view.setOnClickListener { _ ->
            movie?.let {
                listener.onMovieItemClicked(it)
            }
        }
    }

    fun fillDetails(movie: Movie) {
        hideProgress()
        tvTitle.text = movie.title
        tvSeeds.text = String.format(Locale.getDefault(), "%d Seeds", movie.seeds)
        tvPeers.text = String.format(Locale.getDefault(), "%d Peers", movie.peers)
    }

    fun showProgress() {
        pb.visibility = View.VISIBLE
        tvTitle.visibility = View.GONE
        tvSeeds.visibility = View.GONE
        tvPeers.visibility = View.GONE
        card.cardElevation = 0f
    }

    private fun hideProgress() {
        pb.visibility = View.GONE
        tvTitle.visibility = View.VISIBLE
        tvSeeds.visibility = View.VISIBLE
        tvPeers.visibility = View.VISIBLE
        card.cardElevation = 5f
    }

}