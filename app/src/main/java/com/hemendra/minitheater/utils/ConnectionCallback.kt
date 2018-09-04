package com.hemendra.minitheater.utils

import java.net.HttpURLConnection

interface ConnectionCallback {

    fun onConnectionInitialized(conn: HttpURLConnection)
    fun onResponseCode(code: Int)
    fun onInterrupted()
    fun onError()

}