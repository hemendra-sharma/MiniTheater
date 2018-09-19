package com.hemendra.minitheater.data.model.movies

import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.MovieObjectType
import org.json.JSONArray
import org.json.JSONException

class ExtraMoviesScrapper {
    companion object {
        fun moviesHtmlToList(html: String): ArrayList<Movie> {
            if(!html.contains("<tbody>", ignoreCase = true)
                || !html.contains("</tbody>", ignoreCase = true))
                return ArrayList()

            val x = html.indexOf("<tbody>", ignoreCase = true)
            val y = html.lastIndexOf("</tbody>", ignoreCase = true)

            var data = html.substring(x, y+8)

            data = data.replace("\r", "")
            data = data.replace("\n", "")

            data = data.replace("<tbody>", "[")
            data = data.replace("</tbody>", "]")
            data = data.replace(Regex(""" class="[a-z A-Z 0-9 -]*""""), "")
            data = data.replace("<i></i>", "")
            data = data.replace("<tr>", "[")
            data = data.replace("</tr>", "]")
            data = data.replace("][", "],[")
            data = data.replace("</td><td>", "\",\"")
            data = data.replace("<a href=", "")
            data = data.replace("</a>", "")
            data = data.replace("<td>", "\"")
            data = data.replace("</td>", "\"")
            data = data.replace("/\"", "\"")
            data = data.replace("\">", "\"\"")

            while(data.contains("\"\""))
                data = data.replace("\"\"", "\",\"")

            data = data.replace(",\",", ",")
            data = data.replace("[\",", "[")
            data = data.replace("\" \"", "\",\"")
            data = data.replace("] [", "],[")

            data = data.replace(Regex("""<span[^</]+</span>"""), "")
            data = data.replace(",\",", ",")

            /*Utils.writeToFile(data.toByteArray(),
                    File(Environment.getExternalStorageDirectory(), "temp.txt"))*/

            return parseJSON(data)
        }

        private fun parseJSON(json: String): ArrayList<Movie> {
            val moviesList = ArrayList<Movie>()
            try {
                val jsonArray = JSONArray(json)
                val length = jsonArray.length()
                for(i in 0 until length) {
                    val arr = jsonArray.getJSONArray(i)
                    if(arr.length() >= 9) {
                        val movie = Movie()
                        movie.url = arr.optString(1)
                        movie.title = arr.optString(2)
                        movie.seeds = arr.optInt(3)
                        movie.peers = arr.optInt(4)
                        movie.uploader = arr.optString(8)
                        movie.movieObjectType = MovieObjectType.EXTRA
                        moviesList.add(movie)
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
            return moviesList
        }

        fun getMagnetUrlFromHtml(html: String): String {
            if(!html.contains("magnet:", ignoreCase = true))
                return ""

            val x = html.indexOf("magnet:")
            val y = html.indexOf("\"", x)

            return html.substring(x, y)
        }
    }
}