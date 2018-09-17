package com.hemendra.minitheater.view.player

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.text.Html
import android.util.AttributeSet
import android.widget.TextView
import java.io.*

class SubtitlesView: TextView, Runnable {

    constructor(context: Context): super(context)

    constructor(context: Context, attributeSet: AttributeSet): super(context, attributeSet)

    constructor(context: Context,
                attributeSet: AttributeSet,
                style: Int): super(context, attributeSet, style)

    constructor(context: Context,
                attributeSet: AttributeSet?,
                style: Int, styleRes: Int): super(context, attributeSet, style, styleRes)

    private var mediaPlayer: MediaPlayer? = null
    fun setMediaPlayer(mediaPlayer: MediaPlayer) {
        this.mediaPlayer = mediaPlayer
    }

    private var subtitlesFile: File? = null
    private var items = ArrayList<Line>()

    fun setSubtitlesFile(file: File) {
        this.subtitlesFile = file
        loadItems()
    }

    private fun loadItems() {
        try {
            items.clear()
            val input = FileInputStream(subtitlesFile)
            val reader = LineNumberReader(InputStreamReader(input))
            reader.readLine() // read number
            var line = reader.readLine()
            while (line != null) {
                val timeString = line
                val sb = StringBuilder()
                line = reader.readLine()
                while (line != null && !line.isEmpty()) {
                    sb.append(line).append("<br>")
                    line = reader.readLine()
                }

                if(timeString.contains("-->")) {
                    val startTime = parseTime(timeString.split("-->")[0])
                    val endTime = parseTime(timeString.split("-->")[1])

                    if (startTime > 0 && endTime > 0)
                        items.add(Line(startTime, endTime, sb.toString()))
                }

                reader.readLine() // read number
                line = reader.readLine()
            }
        }catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun parseTime(time: String): Long {
        return try {
            val hours = java.lang.Long.parseLong(time.split(":")[0].trim())
            val minutes = java.lang.Long.parseLong(time.split(":")[1].trim())
            val seconds = java.lang.Long.parseLong(time.split(":")[2].split(",")[0].trim())
            val millis = java.lang.Long.parseLong(time.split(":")[2].split(",")[1].trim())

            hours * 60 * 60 * 1000 + minutes * 60 * 1000 + seconds * 1000 + millis
        } catch(e: NumberFormatException) {
            e.printStackTrace()
            0
        }
    }

    private fun getTimedText(position: Long): String {
        var text = ""
        for(item in items) {
            if(item.from <= position && position <= item.to) {
                text = item.text
                break
            }
        }
        return text
    }

    override fun run() {
        mediaPlayer?.let {
            val text = getTimedText(it.currentPosition.toLong())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setText(Html.fromHtml(text, 0))
            } else {
                setText(Html.fromHtml(text))
            }
            postDelayed(this, 300)
        }
    }

    fun stop() {
        mediaPlayer = null
    }

    private class Line(var from: Long, var to: Long, var text: String)

}