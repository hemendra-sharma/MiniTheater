package com.hemendra.minitheater.view.explorer

import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.presenter.ImagesPresenter
import com.hemendra.minitheater.view.listeners.ImageLoaderCallback
import kotlinx.android.synthetic.main.fragment_details.*
import java.util.*
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.hemendra.minitheater.data.Torrent
import com.hemendra.minitheater.presenter.DownloadFailureReason
import com.hemendra.minitheater.presenter.DownloadsPresenter
import com.hemendra.minitheater.view.listeners.OnMovieDownloadClickListener
import com.hemendra.minitheater.view.showMessage


class DetailsFragment: Fragment() {

    private var movie: Movie? = null

    fun setMovie(movie: Movie) {
        this.movie = movie
    }

    private var onMovieDownloadClickListener: OnMovieDownloadClickListener? = null

    fun setMovieDownloadClickListener(listener: OnMovieDownloadClickListener) {
        this.onMovieDownloadClickListener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        movie ?: return
        context ?: return

        tvTitle.text = movie?.title
        ImagesPresenter.getInstance(context!!)
                .loadCoverImage(movie!!, object : ImageLoaderCallback {
            override fun onImageLoaded(url: String, image: Bitmap) {
                ivCover.setImageBitmap(image)
            }
        })
        tvRating.text = String.format(Locale.getDefault(), "%.1f/10", movie?.rating)
        tvYear.text = movie?.year.toString()
        tvLanguage.text = movie?.language
        tvMpaaRating.text = movie?.mpa_rating
        tvDescription.text = movie?.description_full

        if(movie?.genres?.size ?: 0 == 0) tvGenres.visibility = View.GONE
        else fillGenres()

        if(movie?.yt_trailer_code?.isNotEmpty() == true)
            tvTrailer.setOnClickListener(watchTrailerClicked)
        else
            tvTrailer.visibility = View.GONE

        fillTorrentsLayout()
    }

    private fun fillGenres() {
        movie?.let {
            val sb = StringBuilder()
            for(i in 0 until it.genres.size) {
                sb.append(it.genres[i])
                if(i < it.genres.size-1)
                    sb.append(", ")
            }
            tvGenres.text = sb.toString()
        }
    }

    private fun fillTorrentsLayout() {
        llTorrents.removeAllViews()
        movie?.let {
            val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(10, 10, 10, 10)
            for(torrent in it.torrents) {
                val view = View.inflate(context, R.layout.torrent_list_item, null)
                val tvQuality: TextView = view.findViewById(R.id.tvQuality)
                val tvSeeds: TextView = view.findViewById(R.id.tvSeeds)
                val tvPeers: TextView = view.findViewById(R.id.tvPeers)
                val tvDownload: TextView = view.findViewById(R.id.tvDownload)
                val tvPlay: TextView = view.findViewById(R.id.tvPlay)

                tvQuality.text = torrent.quality
                tvSeeds.text = String.format(Locale.getDefault(), "%d Seeds", torrent.seeds)
                tvPeers.text = String.format(Locale.getDefault(), "%d Peers", torrent.peers)
                tvDownload.text = torrent.size

                tvDownload.tag = torrent
                tvPlay.tag = torrent

                tvDownload.setOnClickListener(onDownloadClicked)
                tvPlay.setOnClickListener(onPlayClicked)

                llTorrents.addView(view, params)
            }
        }
    }

    private val watchTrailerClicked = View.OnClickListener { _ ->
        movie?.let {
            val movieID = it.yt_trailer_code
            try {
                val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$movieID"))
                context?.startActivity(appIntent)
            } catch (ex: ActivityNotFoundException) {
                val webIntent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://www.youtube.com/watch?v=$movieID"))
                context?.startActivity(webIntent)
            }
        }
    }

    private val onDownloadClicked = View.OnClickListener { view ->
        movie?.let {
            val m = it.clone()
            m.torrents.clear()
            view.tag?.let { t ->
                m.torrents.add(t as Torrent)
                onMovieDownloadClickListener?.onDownloadClicked(m)
            }
        }
    }

    private val onPlayClicked = View.OnClickListener { view ->
        movie?.let {
            val m = it.clone()
            m.torrents.clear()
            view.tag?.let { t ->
                m.torrents.add(t as Torrent)
                context?.let { ctx ->
                    var ret = DownloadsPresenter.getInstance().addDownload(m)
                    if(ret == DownloadFailureReason.NONE) {
                        ret = DownloadsPresenter.getInstance().startDownload(ctx, m, true)
                        if (ret == DownloadFailureReason.NONE)
                            onMovieDownloadClickListener?.onPlayClicked(m)
                        else
                            showMessage(ctx, "Failed to Start Download ! Reason: $ret")
                    } else
                        showMessage(ctx, "Failed to Add Download ! Reason: $ret")
                }
            }
        }
    }
}