package com.hemendra.minitheater.data.model.subtitles

import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.Subtitle
import com.hemendra.minitheater.presenter.listeners.SubtitlesListDownloadListener
import com.hemendra.minitheater.utils.ConnectionCallback
import com.hemendra.minitheater.utils.ContentDownloader
import com.hemendra.minitheater.utils.CustomAsyncTask
import java.net.HttpURLConnection

class SubtitlesListDownloader(val listener: SubtitlesListDownloadListener):
        CustomAsyncTask<Movie, Void, ArrayList<Subtitle>?>() {

    override fun doInBackground(vararg params: Movie): ArrayList<Subtitle>? {
        val movie = params[0]
        val url = "http://api.yifysubtitles.com/movie-imdb/${movie.imdb_code}"
        val html = ContentDownloader.getString(url, object: ConnectionCallback{
            override fun onConnectionInitialized(conn: HttpURLConnection) {}
            override fun onResponseCode(code: Int) {}
            override fun onInterrupted() {}
            override fun onError() {}
        })
        return if(html != null) {
            YifySubtitlesScrapper.getSubtitlesFromHtml(html)
        } else null
    }

    override fun onPostExecute(result: ArrayList<Subtitle>?) {
        result?.let { listener.onListDownloaded(it) } ?: listener.onFailedToDownloadList()
    }

}