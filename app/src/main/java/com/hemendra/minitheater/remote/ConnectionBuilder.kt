package com.hemendra.minitheater.remote

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class ConnectionBuilder {

    companion object {

        fun getConnection(url: String, method: String): HttpURLConnection? {
            try {
                val conn: HttpURLConnection =
                        if (url.startsWith("https://"))
                            URL(url).openConnection() as HttpsURLConnection
                        else
                            URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 30000
                conn.readTimeout = 30000
                conn.requestMethod = method
                if (method == "POST") conn.doOutput = true
                conn.doInput = true
                return conn
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }

    }

}