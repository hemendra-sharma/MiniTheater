package com.hemendra.minitheater.utils

import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.net.URLEncoder

class RemoteConfig private constructor() {

    companion object {
        private val instance = RemoteConfig()
        fun getInstance(): RemoteConfig {
            return instance
        }
    }

    private val resultsPerPage: Int = 10

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private var isInitialized: Boolean = false

    private var onInitialized: Runnable? = null
    private var onInitializationFailed: Runnable? = null

    private var headerParameterName: String = ""
    private var headerParameterValue: String = ""

    private var allMoviesURL: String = ""
    private var allMoviesPageParameterName: String = ""
    private var allMoviesLimitParameterName: String = ""
    private var allMoviesQueryParameterName: String = ""
    private var allMoviesSortingParameterName: String = ""

    private var imageURL: String = ""
    private var imagePartToExclude: String = ""
    private var imageStringToReplace: String = ""

    private var torrentURL: String = ""
    private var torrentStringToReplace = ""

    private var listener: OnCompleteListener<QuerySnapshot> = OnCompleteListener {
        if(it.isSuccessful) {
            for(document in it.result) {
                when(document.id) {
                    "key" -> {
                        headerParameterName = document["param"] as String
                        headerParameterValue = document["key"] as String
                    }
                    "all_movies" -> {
                        allMoviesURL = document["all_movies_url"] as String
                        allMoviesPageParameterName = document["all_movies_param_page"] as String
                        allMoviesLimitParameterName = document["all_movies_param_limit"] as String
                        allMoviesQueryParameterName = document["all_movies_param_query"] as String
                        allMoviesSortingParameterName = document["all_movies_param_sort"] as String
                    }
                    "get_image" -> {
                        imageURL = document["get_image_url"] as String
                        imagePartToExclude = document["get_image_part_to_exclude"] as String
                        imageStringToReplace = document["get_image_string_to_replace"] as String
                    }
                    "get_torrent" -> {
                        torrentURL = document["url"] as String
                        torrentStringToReplace = document["string_to_replace"] as String
                    }
                }
            }
            isInitialized = true
            onInitialized?.run()
        } else {
            onInitializationFailed?.run()
        }
    }

    fun initialize(onInitialized: Runnable?,
                          onInitializationFailed: Runnable?) {
        this.onInitialized = onInitialized
        this.onInitializationFailed = onInitializationFailed
        db.collection("urls").get().addOnCompleteListener(listener)
    }

    fun getSecurityHeaderKey(): String = headerParameterName

    fun getSecurityHeaderValue(): String = headerParameterValue

    fun getMovieSearchURL(query: String, pageNumber: Int): String {
        if(!isInitialized) throw(IllegalStateException("Initialization not completed yet !"))

        val encQuery = URLEncoder.encode(query, "UTF-8")

        val sb = StringBuilder()
        sb.append(allMoviesURL)
        sb.append("?$allMoviesLimitParameterName=$resultsPerPage")
        sb.append("&$allMoviesPageParameterName=$pageNumber")
        sb.append("&$allMoviesQueryParameterName=$encQuery")
        sb.append("&$allMoviesSortingParameterName=download_count")
        return sb.toString()
    }

    fun getConvertedImageURL(url: String): String {
        if(!isInitialized) throw(IllegalStateException("Initialization not completed yet !"))

        val path = url.replace(imagePartToExclude, "")
        return imageURL.replace(imageStringToReplace, path)
    }

}