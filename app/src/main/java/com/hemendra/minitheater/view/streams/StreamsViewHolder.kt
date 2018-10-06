package com.hemendra.minitheater.view.streams

import android.graphics.Bitmap
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.presenter.ImagesPresenter
import com.hemendra.minitheater.view.listeners.ImageLoaderCallback
import com.hemendra.minitheater.view.listeners.OnStreamItemClickListener
import java.util.*

class StreamsViewHolder(view: View,
                        private val listener: OnStreamItemClickListener,
                        private val imagesPresenter: ImagesPresenter):
        RecyclerView.ViewHolder(view), ImageLoaderCallback {

    private val ivCover: ImageView = view.findViewById(R.id.ivCover)
    private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
    private val ivStar: ImageView = view.findViewById(R.id.ivStar)
    private val tvRating: TextView = view.findViewById(R.id.tvRating)
    private val tvDescription: TextView = view.findViewById(R.id.tvDescription)
    private val tvYear: TextView = view.findViewById(R.id.tvYear)
    var movie: Movie? = null

    init {
        view.setOnClickListener { _ ->
            movie?.let {
                listener.onItemClick(it)
            }
        }
    }

    private var loadingImageURL = ""

    fun fillDetails(movie: Movie) {
        this.movie = movie
        ivCover.setImageBitmap(null)
        loadingImageURL = imagesPresenter.loadCoverImage(movie, this)
        tvTitle.text = movie.title
        if(movie.rating > 0) {
            tvRating.text = String.format(Locale.getDefault(), "%.1f/10", movie.rating)
            tvRating.visibility = View.VISIBLE
            ivStar.visibility = View.VISIBLE
        } else {
            tvRating.visibility = View.GONE
            ivStar.visibility = View.GONE
        }
        if(movie.year > 0) {
            tvYear.text = movie.year.toString()
            tvYear.visibility = View.VISIBLE
        } else {
            tvYear.visibility = View.GONE
        }
        if(movie.description_full.isNotEmpty()) {
            tvDescription.text = movie.description_full
            tvDescription.visibility = View.VISIBLE
        } else {
            tvDescription.visibility = View.GONE
        }
    }

    override fun onImageLoaded(url: String, image: Bitmap) {
        if(url == loadingImageURL) {
            ivCover.setImageBitmap(image)
        }
    }
}