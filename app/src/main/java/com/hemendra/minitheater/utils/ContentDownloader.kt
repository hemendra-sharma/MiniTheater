package com.hemendra.minitheater.utils

import android.support.annotation.WorkerThread
import android.util.Log
import java.io.*
import java.net.HttpURLConnection

class ContentDownloader {

    companion object {

        @WorkerThread
        fun getInputStream(url: String, callback: ConnectionCallback): InputStream? {
            try {
                Log.i("getInputStream", "URL: $url")
                val conn: HttpURLConnection? = ConnectionBuilder.getConnection(url, "GET")
                conn?.let {
                    it.setRequestProperty(RemoteConfig.getInstance().getSecurityHeaderKey(),
                            RemoteConfig.getInstance().getSecurityHeaderValue())
                    callback.onConnectionInitialized(it)
                    it.connect()
                    callback.onResponseCode(it.responseCode)
                    if(it.responseCode == HttpURLConnection.HTTP_OK) {
                        return it.inputStream
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

        @WorkerThread
        fun getByteArray(url: String, callback: ConnectionCallback): ByteArray? {
            var bytes: ByteArray? = null
            var bin: BufferedInputStream? = null
            var out: ByteArrayOutputStream? = null
            try {
                Log.i("getByteArray", "URL: $url")
                val conn: HttpURLConnection? = ConnectionBuilder.getConnection(url, "GET")
                conn?.let {
                    it.setRequestProperty(RemoteConfig.getInstance().getSecurityHeaderKey(),
                            RemoteConfig.getInstance().getSecurityHeaderValue())
                    callback.onConnectionInitialized(it)
                    it.connect()
                    callback.onResponseCode(it.responseCode)
                    if(it.responseCode == HttpURLConnection.HTTP_OK) {
                        bin = BufferedInputStream(it.inputStream)
                        out = ByteArrayOutputStream()
                        val arr = ByteArray(1024)
                        var read = bin!!.read(arr)
                        while(read > 0) {
                            out!!.write(arr, 0, read)
                            read = bin!!.read(arr)
                        }
                        bin!!.close()
                        bytes = out!!.toByteArray()
                        out!!.close()
                    }
                }
            } catch (e: InterruptedIOException) {
                callback.onInterrupted()
            } catch (e: IOException) {
                e.printStackTrace()
                callback.onError()
            } finally {
                bin?.close()
                out?.close()
            }
            return bytes
        }

    }

}