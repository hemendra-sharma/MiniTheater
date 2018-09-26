package com.hemendra.minitheater.data.model.movies

import android.util.JsonReader
import android.util.JsonToken
import android.util.MalformedJsonException
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.Torrent
import org.json.JSONException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

class MoviesParser {

    companion object {

        fun parseStream(stream: InputStream): ArrayList<Movie> {
            val movies: ArrayList<Movie> = ArrayList()
            var reader: JsonReader? = null
            try {
                reader = JsonReader(InputStreamReader(stream))
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
            } catch(e: RuntimeException) {
                e.printStackTrace()
            } catch (e: JSONException) {
                e.printStackTrace()
            } catch (e: MalformedJsonException) {
                e.printStackTrace()
            } finally {
                reader?.close()
            }
            return movies
        }

        /**
         * This section is just for generating the dummy sreenshots [ copyrights ;) ]
         */
        /*val dummyTitles = ArrayList<String>()
        init {
            dummyTitles.add("Awesome Movie")
            dummyTitles.add("The Best Movie")
            dummyTitles.add("Great Movie")
            dummyTitles.add("Amazing Movie")
            dummyTitles.add("Fantastic Movie")
            dummyTitles.add("The Good One")
            dummyTitles.add("The Bad One")
        }

        val dummyDescriptions = ArrayList<String>()
        init {
            dummyDescriptions.add("""Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                |Praesent interdum blandit purus, viverra porttitor neque imperdiet a.
                |Cras ut elit lacus. Morbi ut dictum libero, eget pulvinar libero.""".trimMargin())
            dummyDescriptions.add("""Nullam viverra neque ullamcorper urna viverra rhoncus.
                |Pellentesque pretium tortor dolor, et fringilla nibh sollicitudin in.
                |Sed eu rutrum quam, in rhoncus ipsum. Pellentesque pulvinar urna in urna
                |pharetra, ac egestas felis cursus.""".trimMargin())
            dummyDescriptions.add("""Ut consequat urna a leo lacinia, ut fringilla nisi mattis.
                |Nunc feugiat commodo tellus, vitae tempus diam imperdiet vitae. Vestibulum ante
                |ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae;
                |Praesent eget nisi in arcu eleifend ullamcorper vitae nec arcu.""".trimMargin())
            dummyDescriptions.add("""Aliquam dui justo, dictum ut elementum eget, interdum vitae
                |justo. Vivamus commodo erat sed vestibulum iaculis. Nunc risus ante, facilisis
                |in dictum eu, laoreet ut urna.""".trimMargin())
            dummyDescriptions.add("""Integer pretium dolor sit amet enim rutrum sollicitudin.
                |Aenean mollis non nisl sed posuere. Aenean tempus, lectus id finibus egestas,
                |nulla dui sollicitudin est, sit amet auctor lorem tortor ac justo.""".trimMargin())
            dummyDescriptions.add("""Pellentesque habitant morbi tristique senectus et netus et
                |malesuada fames ac turpis egestas. Morbi ultricies est condimentum justo
                |hendrerit dapibus.""".trimMargin())
            dummyDescriptions.add("""Suspendisse eu dictum lacus. In justo turpis, tempor id
                |lacus non, tincidunt fringilla justo. Pellentesque sollicitudin, ante ut
                |pharetra tincidunt, nisi est mattis est, ac volutpat enim libero sit
                |amet magna.""".trimMargin())
        }
        private var tempIndex = 0*/

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

            /**
             * This section is just for generating the dummy sreenshots [ copyrights ;) ]
             */
            /*movie.title = dummyTitles[tempIndex]
            movie.description_full = dummyDescriptions[tempIndex]
            tempIndex++
            if(tempIndex >= 7) tempIndex = 0*/

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