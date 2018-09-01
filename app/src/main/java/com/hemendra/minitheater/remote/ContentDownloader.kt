package com.hemendra.minitheater.remote

import android.support.annotation.WorkerThread
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.net.HttpURLConnection

class ContentDownloader {

    companion object {

        @WorkerThread
        fun getInputStream(url: String, callback: ConnectionCallback): InputStream? {
            try {
                Log.i("getInputStream", "URL: $url")
                val conn: HttpURLConnection? = ConnectionBuilder.getConnection(url, "GET")
                conn?.let {
                    conn.setRequestProperty(RemoteConfig.getInstance().getSecurityHeaderKey(),
                            RemoteConfig.getInstance().getSecurityHeaderValue())
                    callback.onConnectionInitialized(conn)
                    conn.connect()
                    callback.onResponseCode(conn.responseCode)
                    if(conn.responseCode == HttpURLConnection.HTTP_OK) {
                        return conn.inputStream
                    }
                }
            } catch (e: InterruptedIOException) {
                callback.onInterrupted()
            } catch (e: IOException) {
                e.printStackTrace()
                callback.onError()
            }
            return null
        }

    }

}