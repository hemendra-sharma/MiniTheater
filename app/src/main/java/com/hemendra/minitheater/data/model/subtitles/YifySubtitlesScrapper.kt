package com.hemendra.minitheater.data.model.subtitles

import com.hemendra.minitheater.data.Subtitle
import org.json.JSONArray
import org.json.JSONException

class YifySubtitlesScrapper {
    companion object {
        fun getSubtitlesFromHtml(html: String): ArrayList<Subtitle>? {
            val x = html.indexOf("<tbody>", ignoreCase = true)
            val y = html.lastIndexOf("</tbody>", ignoreCase = true)

            var data = html.substring(x, y+8)

            data = data.replace("<tbody>", "[")
            data = data.replace("""</tbody>""", "]")
            data = data.replace(Regex(""" class=[^a-zA-Z -].*?[^a-zA-Z -]"""), "")
            data = data.replace("<span>", "")
            data = data.replace("""</span>""", "")
            data = data.replace("""</td><td>""", "\",\"")
            data = data.replace("<td>", "\"")
            data = data.replace("\"<a href=\"", "\"")
            data = data.replace("\">subtitle", "\",\"")
            data = data.replace("</a>\"", "\"")
            data = data.replace(Regex("\"/user.+?\","), "")
            data = data.replace(""">download</a></td>""", "")
            data = data.replace("<tr ", "[")
            data = data.replace("</tr>", "]")
            data = data.replace(Regex("""data-id=".+?">"""), "")
            data = data.replace(Regex(""""<span.+?>","""), "")
            data = data.replace(""","",""", ",")
            data = data.replace("""][""", """],[""")

            return parseJSON(data)
        }

        private fun parseJSON(json: String): ArrayList<Subtitle>? {
            try {
                val subtitlesList = ArrayList<Subtitle>()
                val jsonArray = JSONArray(json)
                val length = jsonArray.length()
                for(i in 0 until length) {
                    val arr = jsonArray.getJSONArray(i)
                    if(arr.length() == 5) {
                        val count = arr.getInt(0)
                        val language = arr.getString(1)
                        val name = arr.getString(3)
                        val path = arr.getString(4)
                        subtitlesList.add(Subtitle(count, language, name, path))
                    }
                }
                return subtitlesList
            } catch (e: JSONException) {
                e.printStackTrace()
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
            return null
        }

        fun getZipFileURL(html: String): String {
            val x = html.indexOf("http://www.yifysubtitles.com/subtitle/")
            val y = html.indexOf("\"><span class=\"icon32 download\">")
            return html.substring(x, y)
        }
    }
}