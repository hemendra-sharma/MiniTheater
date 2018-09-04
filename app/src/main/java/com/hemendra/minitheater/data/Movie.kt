package com.hemendra.minitheater.data

import java.io.Serializable

class Movie: Serializable, Cloneable {

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
    var rating: Double = 0.0
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

    var isDownloading: Boolean = false

    public override fun clone(): Movie {
        return super.clone() as Movie
    }

    override fun equals(other: Any?): Boolean {
        other?.let { return (it is Movie) && (it.id == id) }
        return false
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + url.hashCode()
        result = 31 * result + imdb_code.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + title_english.hashCode()
        result = 31 * result + title_long.hashCode()
        result = 31 * result + slug.hashCode()
        result = 31 * result + year
        result = 31 * result + rating.hashCode()
        result = 31 * result + runtime
        result = 31 * result + genres.hashCode()
        result = 31 * result + summary.hashCode()
        result = 31 * result + description_full.hashCode()
        result = 31 * result + synopsis.hashCode()
        result = 31 * result + yt_trailer_code.hashCode()
        result = 31 * result + language.hashCode()
        result = 31 * result + mpa_rating.hashCode()
        result = 31 * result + background_image.hashCode()
        result = 31 * result + background_image_original.hashCode()
        result = 31 * result + small_cover_image.hashCode()
        result = 31 * result + medium_cover_image.hashCode()
        result = 31 * result + large_cover_image.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + torrents.hashCode()
        result = 31 * result + date_uploaded.hashCode()
        result = 31 * result + date_uploaded_unix.hashCode()
        return result
    }

}