package com.hemendra.minitheater.model.movies

import android.util.JsonReader
import android.util.JsonToken
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.Torrent
import org.json.JSONException
import java.io.InputStream
import java.io.InputStreamReader

class MoviesParser {

    companion object {

        fun parseStream(stream: InputStream): ArrayList<Movie> {
            val movies: ArrayList<Movie> = ArrayList()
            try {
                val reader = JsonReader(InputStreamReader(stream))
                reader.beginObject()
                while(reader.hasNext()) {
                    if(reader.peek() == JsonToken.NULL) {
                        reader.skipValue()
                        continue
                    }
                    //
                    if(reader.nextName() == "data") {
                        reader.beginObject()
                        while(reader.hasNext()) {
                            if(reader.peek() == JsonToken.NULL) {
                                reader.skipValue()
                                continue
                            }
                            //
                            if(reader.nextName() == "movies") {
                                reader.beginArray()
                                while(reader.hasNext()) {
                                    if(reader.peek() == JsonToken.NULL) {
                                        reader.skipValue()
                                        continue
                                    }
                                    //
                                    movies.add(parseMovie(reader))
                                }
                                reader.endArray()
                            } else {
                                reader.skipValue()
                            }
                        }
                        reader.endObject()
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }catch (e: JSONException) {
                e.printStackTrace()
            }
            return movies
        }

        private fun parseMovie(reader: JsonReader): Movie {
            val movie = Movie()
            reader.beginObject()
            while(reader.hasNext()) {
                when (reader.nextName()) {
                    "id" -> movie.id = reader.nextInt()
                    "url" -> movie.url = reader.nextString()
                    "imdb_code" -> movie.imdb_code = reader.nextString()
                    "title" -> movie.title = reader.nextString()
                    "title_english" -> movie.title_english = reader.nextString()
                    "title_long" -> movie.title_long = reader.nextString()
                    "slug" -> movie.slug = reader.nextString()
                    "year" -> movie.year = reader.nextInt()
                    "rating" -> movie.rating = reader.nextDouble()
                    "runtime" -> movie.runtime = reader.nextInt()
                    "genres" -> {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            if (reader.peek() == JsonToken.NULL) {
                                reader.skipValue()
                                continue
                            }
                            movie.genres.add(reader.nextString())
                        }
                        reader.endArray()
                    }
                    "summary" -> movie.summary = reader.nextString()
                    "description_full" -> movie.description_full = reader.nextString()
                    "synopsis" -> movie.synopsis = reader.nextString()
                    "yt_trailer_code" -> movie.yt_trailer_code = reader.nextString()
                    "language" -> movie.language = reader.nextString()
                    "mpa_rating" -> movie.mpa_rating = reader.nextString()
                    "background_image" -> movie.background_image = reader.nextString()
                    "background_image_original" -> movie.background_image_original = reader.nextString()
                    "small_cover_image" -> movie.small_cover_image = reader.nextString()
                    "medium_cover_image" -> movie.medium_cover_image = reader.nextString()
                    "large_cover_image" -> movie.large_cover_image = reader.nextString()
                    "state" -> movie.state = reader.nextString()
                    "torrents" -> {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            if (reader.peek() == JsonToken.NULL) {
                                reader.skipValue()
                                continue
                            }
                            movie.torrents.add(parseTorrent(reader))
                        }
                        reader.endArray()
                    }
                    "date_uploaded" -> movie.date_uploaded = reader.nextString()
                    "date_uploaded_unix" -> movie.date_uploaded_unix = reader.nextLong()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            return movie
        }

        private fun parseTorrent(reader: JsonReader): Torrent {
            val torrent = Torrent()
            reader.beginObject()
            while(reader.hasNext()) {
                if(reader.peek() == JsonToken.NULL) {
                    reader.skipValue()
                    continue
                }
                when(reader.nextName()) {
                    "url" -> torrent.url = reader.nextString()
                    "hash" -> torrent.hash = reader.nextString()
                    "quality" -> torrent.quality = reader.nextString()
                    "seeds" -> torrent.seeds = reader.nextInt()
                    "peers" -> torrent.peers = reader.nextInt()
                    "size" -> torrent.size = reader.nextString()
                    "size_bytes" -> torrent.size_bytes = reader.nextLong()
                    "date_uploaded" -> torrent.date_uploaded = reader.nextString()
                    "date_uploaded_unix" -> torrent.date_uploaded_unix = reader.nextLong()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            return torrent
        }

    }

}