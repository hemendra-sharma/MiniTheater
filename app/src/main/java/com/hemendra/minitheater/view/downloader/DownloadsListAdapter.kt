package com.hemendra.minitheater.view.downloader

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.presenter.ImagesPresenter
import com.hemendra.minitheater.view.listeners.OnDownloadItemClickListener
import java.util.ArrayList

class DownloadsListAdapter(var movies: ArrayList<Movie>,
                           private val listener: OnDownloadItemClickListener):
        RecyclerView.Adapter<DownloadViewHolder>() {

    override fun onCreateViewHolder(vg: ViewGroup, position: Int): DownloadViewHolder {
        val view = LayoutInflater.from(vg.context)
                .inflate(R.layout.download_list_item, vg, false)
        return DownloadViewHolder(view, listener, ImagesPresenter.getInstance(vg.context))
    }

    override fun getItemCount(): Int = movies.size

    override fun onBindViewHolder(holder: DownloadViewHolder, i: Int) {
        holder.fillDetails(movies[i])
    }

    override fun onViewAttachedToWindow(holder: DownloadViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.registerReceiver()
    }

    override fun onViewDetachedFromWindow(holder: DownloadViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.unregisterReceiver()
    }

}