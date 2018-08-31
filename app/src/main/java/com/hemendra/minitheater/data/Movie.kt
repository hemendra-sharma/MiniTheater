package com.hemendra.minitheater.data

import java.io.Serializable

class Movie: Serializable {

    companion object {
        const val serialVersionUID: Long = 2348092384L
    }

    var id: Int = 0
    var url: String = ""
    var imdb_code: String = ""
    var title: String = "-"
    var title_english: String = "-"
    var title_long: String = ""
    var slug: String = ""
    var year: Int = 0
    var rating: Int = 0
    var runtime: Int = 0
    var genres: ArrayList<String> = ArrayList()
    var summary: String = ""
    var description_full: String = ""
    var synopsis: String = ""
    var yt_trailer_code: String = ""
    var language: String = ""
    var mpa_rating: String = ""
    var background_image: String = ""
    var background_image_original: String = ""
    var small_cover_image: String = ""
    var medium_cover_image: String = ""
    var large_cover_image: String = ""
    var state: String = ""
    var torrents: ArrayList<Torrent> = ArrayList()
    var date_uploaded: String = ""
    var date_uploaded_unix: Long = 0

}