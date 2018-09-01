package com.hemendra.minitheater.model.movies

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.remote.ConnectionCallback
import com.hemendra.minitheater.remote.ContentDownloader
import com.hemendra.minitheater.remote.RemoteConfig
import java.io.InputStream
import java.net.HttpURLConnection

class MoviesLoader(private var listener: IMoviesLoaderListener):
        AsyncTask<Any,Void,ArrayList<Movie>?>() {

    private var connection: HttpURLConnection? = null
    private var reason: MoviesDataSourceFailureReason = MoviesDataSourceFailureReason.UNKNOWN

    private var disconnectHandler: Handler? = null

    private val disconnectCallback: Handler.Callback = Handler.Callback {
        connection?.disconnect()
        true
    }

    fun stop(interrupt: Boolean) {
        cancel(interrupt)
        disconnectHandler?.sendEmptyMessage(0)
    }

    override fun doInBackground(vararg params: Any): ArrayList<Movie>? {
        Looper.prepare()

        val url: String = RemoteConfig.getInstance()
                .getMovieSearchURL(params[0] as String, params[1] as Int)
        val stream: InputStream = ContentDownloader.getInputStream(url, object: ConnectionCallback{

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

        val movies = MoviesParser.parseStream(stream)

        if(isCancelled) {
            reason = MoviesDataSourceFailureReason.ABORTED
            return null
        }

        return movies
    }

    override fun onPostExecute(result: ArrayList<Movie>?) {
        if(result != null) {
            if(result.isEmpty())
                listener.onFailedToLoadMovies(MoviesDataSourceFailureReason.NO_SEARCH_RESULTS)
            else
                listener.onMoviesLoaded(result)
        } else {
            listener.onFailedToLoadMovies(reason)
        }
    }
}