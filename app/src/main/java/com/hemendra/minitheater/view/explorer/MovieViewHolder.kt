package com.hemendra.minitheater.view.explorer

import android.graphics.Bitmap
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.presenter.ImagesPresenter
import com.hemendra.minitheater.view.listeners.ImageLoaderCallback
import com.hemendra.minitheater.view.listeners.OnMovieItemClickListener
import java.util.*

class MovieViewHolder(view: View, private val listener: OnMovieItemClickListener,
                      private val imagesPresenter: ImagesPresenter):
        RecyclerView.ViewHolder(view), ImageLoaderCallback {

    private val card: CardView = view as CardView
    private val ivCover: ImageView = view.findViewById(R.id.ivCover)
    private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
    private val ivStar: ImageView = view.findViewById(R.id.ivStar)
    private val tvRating: TextView = view.findViewById(R.id.tvRating)
    private val tvDescription: TextView = view.findViewById(R.id.tvDescription)
    private val tvYear: TextView = view.findViewById(R.id.tvYear)
    private val pb: ProgressBar = view.findViewById(R.id.pb)
    var movie: Movie? = null

    init {
        view.setOnClickListener { _ ->
            movie?.let {
                listener.onMovieItemClicked(it)
            }
        }
    }

    private var loadingImageURL = ""

    fun fillDetails(movie: Movie) {
        hideProgress()
        ivCover.setImageBitmap(null)
        loadingImageURL = imagesPresenter.loadCoverImage(movie, this)
        tvTitle.text = movie.title
        tvRating.text = String.format(Locale.getDefault(), "%.1f/10", movie.rating)
        tvYear.text = movie.year.toString()
        tvDescription.text = movie.description_full
    }

    fun showProgress() {
        pb.visibility = View.VISIBLE
        ivCover.visibility = View.GONE
        tvTitle.visibility = View.GONE
        ivStar.visibility = View.GONE
        tvRating.visibility = View.GONE
        tvYear.visibility = View.GONE
        tvDescription.visibility = View.GONE
        card.cardElevation = 0f
    }

    private fun hideProgress() {
        pb.visibility = View.GONE
        ivCover.visibility = View.VISIBLE
        tvTitle.visibility = View.VISIBLE
        ivStar.visibility = View.VISIBLE
        tvRating.visibility = View.VISIBLE
        tvYear.visibility = View.VISIBLE
        tvDescription.visibility = View.VISIBLE
        card.cardElevation = 5f
    }

    override fun onImageLoaded(url: String, image: Bitmap) {
        if(url == loadingImageURL) {
            ivCover.setImageBitmap(image)
        }
    }
}