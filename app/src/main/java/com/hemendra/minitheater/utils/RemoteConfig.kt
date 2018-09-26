package com.hemendra.minitheater.utils

import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.Torrent
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
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
    private var allMoviesGenreParameterName: String = ""
    private var allMoviesSortingParameterDefaultValue: String = ""
    private var allMoviesGenreParameterDefaultValue: String = ""
    private var allMoviesSortingOptionsJSON: String = ""
    private var allMoviesGenresListJSON: String = ""

    private var imageURL: String = ""
    private var imagePartToExclude: String = ""
    private var imageStringToReplace: String = ""

    private var torrentURL: String = ""
    private var torrentStringToReplace = ""

    private var magnetURL: String = ""
    private var magnetReplaceHash: String = ""
    private var magnetReplaceMovieName: String = ""

    private var extraMovieSearchURL: String = ""
    private var extraMovieQueryParam: String = ""
    private var extraMoviePageParam: String = ""
    private var extraMovieRedirectReplace: String = ""
    private var extraMovieRedirectReplaceWith: String = ""

    private var shareLink: String = ""
    private var shareMessage: String = ""
    private var sharePositiveText: String = ""
    private var shareNegativeText: String = ""

    private var updateLink: String = ""
    private var updateMessage: String = ""
    private var updateVersionCode: Long = 0

    private val gotList = ArrayList<String>()

    private var listener: OnCompleteListener<QuerySnapshot> = OnCompleteListener {
        if(it.isSuccessful) {
            for(document in it.result) {
                if(!gotList.contains(document.id)) gotList.add(document.id)

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
                        allMoviesGenreParameterName = document["all_movies_param_genre"] as String
                        allMoviesSortingParameterDefaultValue =
                                document["all_movies_param_sort_default"] as String
                        allMoviesGenreParameterDefaultValue =
                                document["all_movies_param_genre_default"] as String
                        allMoviesSortingOptionsJSON =
                                document["all_movies_param_sort_list"] as String
                        allMoviesGenresListJSON =
                                document["all_movies_param_genre_list"] as String
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
                    "magnet" -> {
                        magnetURL = document["magnet_url"] as String
                        magnetReplaceHash = document["hash_replace"] as String
                        magnetReplaceMovieName = document["movie_name_replace"] as String
                    }
                    "extra_movies" -> {
                        extraMovieSearchURL = document["extra_movies_search_url"] as String
                        extraMovieQueryParam = document["extra_movies_query_param"] as String
                        extraMoviePageParam = document["extra_movies_page_param"] as String
                        extraMovieRedirectReplace =
                                document["extra_movies_redirect_replace"] as String
                        extraMovieRedirectReplaceWith =
                                document["extra_movies_redirect_replace_with"] as String
                    }
                    "share" -> {
                        shareLink = document["share_link"] as String
                        shareMessage = document["share_message"] as String
                        sharePositiveText = document["share_positive_text"] as String
                        shareNegativeText = document["share_negative_text"] as String
                    }
                    "update" -> {
                        updateLink = document["update_link"] as String
                        updateMessage = document["update_message"] as String
                        updateVersionCode = document["update_version_code"] as Long
                    }
                }
            }
            if(gotList.size >= 8) {
                isInitialized = true
                onInitialized?.run()
                onInitialized = null
            }
        } else {
            onInitializationFailed?.run()
        }
    }

    fun initialize(onInitialized: Runnable?,
                          onInitializationFailed: Runnable?) {
        this.onInitialized = onInitialized
        this.onInitializationFailed = onInitializationFailed
        db.collection("urls").get().addOnCompleteListener(listener)
        db.collection("messages").get().addOnCompleteListener(listener)
    }

    fun getSecurityHeaderKey(): String = headerParameterName

    fun getSecurityHeaderValue(): String = headerParameterValue

    fun getMovieSearchURL(query: String, pageNumber: Int,
                          sortBy: String = allMoviesSortingParameterDefaultValue,
                          genre: String = allMoviesGenreParameterDefaultValue): String {
        if(!isInitialized) throw(IllegalStateException("Initialization not completed yet !"))

        val encQuery = URLEncoder.encode(query, "UTF-8")

        var sortByParam = sortBy
        if(sortBy.isEmpty()) sortByParam = allMoviesSortingParameterDefaultValue

        var genreParam = genre
        if(genre.isEmpty()) genreParam = allMoviesGenreParameterDefaultValue

        val sb = StringBuilder()
        sb.append(allMoviesURL)
        sb.append("?$allMoviesLimitParameterName=$resultsPerPage")
        sb.append("&$allMoviesPageParameterName=$pageNumber")
        sb.append("&$allMoviesQueryParameterName=$encQuery")
        sb.append("&$allMoviesSortingParameterName=$sortByParam")
        sb.append("&$allMoviesGenreParameterName=${genreParam.toLowerCase()}")
        return sb.toString()
    }

    fun getExtraMovieSearchURL(query: String, pageNumber: Int): String {
        if(!isInitialized) throw(IllegalStateException("Initialization not completed yet !"))

        val encQuery = URLEncoder.encode(query, "UTF-8")

        return extraMovieSearchURL.replace(extraMovieQueryParam, encQuery)
                .replace(extraMoviePageParam, pageNumber.toString())
    }

    fun getExtraMovieRedirectUrl(url: String): String {
        return if(url.contains(extraMovieRedirectReplace))
            url.replace(extraMovieRedirectReplace, extraMovieRedirectReplaceWith)
        else
            extraMovieRedirectReplaceWith + url
    }

    fun getConvertedImageURL(url: String): String {
        if(!isInitialized) throw(IllegalStateException("Initialization not completed yet !"))

        val path = url.replace(imagePartToExclude, "")
        return imageURL.replace(imageStringToReplace, path)
    }

    fun getTorrentURL(torrent: Torrent): String {
        if(!isInitialized) throw(IllegalStateException("Initialization not completed yet !"))

        return torrentURL.replace(torrentStringToReplace, torrent.hash)
    }

    fun getMagnetURL(movie: Movie): String {
        if(!isInitialized) throw(IllegalStateException("Initialization not completed yet !"))

        assert(movie.torrents.size == 1)

        if(movie.torrents[0].url.startsWith("magnet:")) {
            return movie.torrents[0].url
        } else {
            val torrentHash = movie.torrents[0].hash
            val movieName = URLEncoder.encode(movie.title, "UTF-8")

            return magnetURL.replace(magnetReplaceHash, torrentHash)
                    .replace(magnetReplaceMovieName, movieName)
        }
    }

    fun getGenreOptions(): ArrayList<String> {
        val list = ArrayList<String>()
        try {
            val jsonArray = JSONArray(allMoviesGenresListJSON)
            val length = jsonArray.length()
            for(i in 0 until length) {
                val value = jsonArray.getString(i)
                if(allMoviesGenreParameterDefaultValue != value)
                    list.add(jsonArray.getString(i))
            }
            list.add(0, allMoviesGenreParameterDefaultValue)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return list
    }

    fun getSortingOptionsMap(): HashMap<String,String> {
        val map = HashMap<String,String>()
        try {
            val jsonObject = JSONObject(allMoviesSortingOptionsJSON)
            val keys = jsonObject.keys()
            while(keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonObject.getString(key)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return map
    }

    fun getSortingOptionsList(): ArrayList<String> {
        val list = ArrayList<String>()
        try {
            val jsonObject = JSONObject(allMoviesSortingOptionsJSON)
            val keys = jsonObject.keys()
            while(keys.hasNext()) {
                val key = keys.next()
                if(key != allMoviesSortingParameterDefaultValue)
                    list.add(key)
            }
            list.add(0, allMoviesSortingParameterDefaultValue)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return list
    }

    fun getSharingLink(): String = shareLink

    fun getSharingMessage(): String {
        return shareMessage.replace("\\n", "\n")
    }

    fun getSharingPositiveText(): String = sharePositiveText

    fun getSharingNegativeText(): String = shareNegativeText

    fun getUpdateLink(): String = updateLink

    fun getUpdateMessage(): String {
        return updateMessage.replace("\\n", "\n")
    }

    fun getUpdateVersionCode(): Long = updateVersionCode

}