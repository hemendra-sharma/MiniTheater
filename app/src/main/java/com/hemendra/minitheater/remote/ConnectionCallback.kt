package com.hemendra.minitheater.remote

import java.net.HttpURLConnection

interface ConnectionCallback {

    fun onConnectionInitialized(conn: HttpURLConnection)
    fun onResponseCode(code: Int)
    fun onInterrupted()
    fun onError()

}