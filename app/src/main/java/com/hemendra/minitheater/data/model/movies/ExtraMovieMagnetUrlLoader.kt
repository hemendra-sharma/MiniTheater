package com.hemendra.minitheater.data.model.movies

import android.os.Handler
import android.os.Looper
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.listeners.OnMoviesLoadedListener
import com.hemendra.minitheater.utils.ConnectionCallback
import com.hemendra.minitheater.utils.ContentDownloader
import com.hemendra.minitheater.utils.CustomAsyncTask
import com.hemendra.minitheater.utils.RemoteConfig
import java.net.HttpURLConnection

class ExtraMovieMagnetUrlLoader(private var listener: OnMoviesLoadedListener):
        CustomAsyncTask<Any, Void, String?>() {

    private var connection: HttpURLConnection? = null
    private var reason: MoviesDataSourceFailureReason = MoviesDataSourceFailureReason.UNKNOWN

    private var disconnectHandler: Handler? = null

    private val disconnectCallback: Handler.Callback = Handler.Callback {
        connection?.disconnect()
        true
    }

    override fun cancel(interrupt: Boolean) {
        disconnectHandler?.sendEmptyMessage(0)
        super.cancel(true)
    }

    override fun doInBackground(vararg params: Any): String? {
        if(Looper.myLooper() == null) Looper.prepare()

        var pageURL = params[0] as String
        if(!pageURL.endsWith("/")) pageURL = "$pageURL/"

        val url: String = RemoteConfig.getInstance().getExtraMovieRedirectUrl(pageURL)
        val html: String? = ContentDownloader.getString(url, object: ConnectionCallback{

            override fun onConnectionInitialized(conn: HttpURLConnection) {
                connection = conn
                disconnectHandler = Handler(disconnectCallback)
            }

            override fun onResponseCode(code: Int) {
                when(code) {
                    HttpURLConnection.HTTP_NOT_FOUND ->
                        reason = MoviesDataSourceFailureReason.API_MISSING
                }
            }

            override fun onInterrupted() {
                reason = MoviesDataSourceFailureReason.ABORTED
            }

            override fun onError() {
                reason = MoviesDataSourceFailureReason.NETWORK_TIMEOUT
            }
        }) ?: return null

        if(isCancelled) {
            reason = MoviesDataSourceFailureReason.ABORTED
            return null
        }

        if(isCancelled) {
            reason = MoviesDataSourceFailureReason.ABORTED
            return null
        }

        html?.let {
            reason = MoviesDataSourceFailureReason.NO_SEARCH_RESULTS
            return ExtraMoviesScrapper.getMagnetUrlFromHtml(it)
        } ?: return null
    }

    override fun onPostExecute(result: String?) {
        if(result != null) {
            if(result.isEmpty())
                listener.onFailedToLoadMovies(MoviesDataSourceFailureReason.NO_SEARCH_RESULTS)
            else
                listener.onMagnetURL(result)
        } else {
            listener.onFailedToLoadMovies(reason)
        }
    }
}