package com.hemendra.minitheater.data.model.streams

import android.os.Handler
import android.os.Looper
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.utils.CustomAsyncTask
import com.hemendra.minitheater.utils.Utils
import com.hemendra.minitheater.view.listeners.StreamSearchListener
import java.io.BufferedInputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.net.HttpURLConnection
import java.net.URL

class NetworkStreamSearcher(val listener: StreamSearchListener):
        CustomAsyncTask<Void, Movie, Void?>() {

    private var connection: HttpURLConnection? = null
    private var disconnectHandler: Handler? = null

    private val disconnectCallback: Handler.Callback = Handler.Callback {
        connection?.disconnect()
        true
    }

    override fun cancel(interrupt: Boolean) {
        disconnectHandler?.sendEmptyMessage(0)
        super.cancel(true)
    }

    override fun onProgressUpdate(vararg progress: Movie) {
        listener.onMovieFound(progress[0])
    }

    override fun doInBackground(vararg params: Void): Void? {
        if(Looper.myLooper() == null) Looper.prepare()

        val localIP = Utils.getLocalIpAddress()
        val prefix = localIP.substring(0, localIP.lastIndexOf("."))

        for(i in 0..255) {
            if(isCancelled) return null
            try {
                val url = "http://$prefix.$i:4848/movie"
                val conn: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 100
                conn.readTimeout = 1000
                conn.requestMethod = "GET"
                conn.doInput = true
                connection = conn
                disconnectHandler = Handler(disconnectCallback)
                conn.connect()
                if(conn.responseCode == HttpURLConnection.HTTP_OK
                        || conn.responseCode == HttpURLConnection.HTTP_PARTIAL) {
                    val bufferIn = BufferedInputStream(conn.inputStream)
                    val objIn = ObjectInputStream(bufferIn)
                    val movie = objIn.readObject() as Movie?
                    if(movie != null) {
                        movie.streamingURL = "http://$prefix.$i:4848/stream"
                        publishProgress(movie)
                    }
                    bufferIn.close()
                    objIn.close()
                }
            } catch (e: ClassNotFoundException) {
                //e.printStackTrace()
            } catch (e: IOException) {
                //e.printStackTrace()
            }
        }

        return null
    }

}