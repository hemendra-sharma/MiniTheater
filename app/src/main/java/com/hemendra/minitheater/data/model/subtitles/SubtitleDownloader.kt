package com.hemendra.minitheater.data.model.subtitles

import android.os.Environment
import com.hemendra.minitheater.presenter.listeners.SubtitleDownloadListener
import com.hemendra.minitheater.utils.ConnectionCallback
import com.hemendra.minitheater.utils.ContentDownloader
import com.hemendra.minitheater.utils.CustomAsyncTask
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.net.HttpURLConnection
import java.util.zip.ZipInputStream

class SubtitleDownloader(var listener: SubtitleDownloadListener):
        CustomAsyncTask<String, Void, File?>() {

    override fun doInBackground(vararg params: String): File? {
        try {
            val path = params[0]
            val url = "http://api.yifysubtitles.com$path"
            val html = ContentDownloader.getString(url, object : ConnectionCallback {
                override fun onConnectionInitialized(conn: HttpURLConnection) {}
                override fun onResponseCode(code: Int) {}
                override fun onInterrupted() {}
                override fun onError() {}
            })

            if(isCancelled) return null

            html?.let {
                val zipFileURL = YifySubtitlesScrapper.getZipFileURL(it)
                val stream = ContentDownloader.getInputStream(zipFileURL, object : ConnectionCallback {
                    override fun onConnectionInitialized(conn: HttpURLConnection) {}
                    override fun onResponseCode(code: Int) {}
                    override fun onInterrupted() {}
                    override fun onError() {}
                })

                if(isCancelled) return null

                stream?.let { inputStream ->
                    val zipInputStream = ZipInputStream(inputStream)
                    val nextEntry = zipInputStream.nextEntry
                    var file: File? = null

                    nextEntry?.let { entry ->
                        val dir = File(Environment.getExternalStorageDirectory().absolutePath +
                                "/MiniTheater/subtitles")
                        dir.mkdirs()
                        file = File(dir, entry.name)
                        val out = FileOutputStream(file)

                        val buffer = ByteArray(1024)
                        var read = zipInputStream.read(buffer)
                        while(read != -1 && !isCancelled) {
                            out.write(buffer, 0, read)
                            read = zipInputStream.read(buffer)
                        }
                        inputStream.close()
                        out.close()
                    }
                    zipInputStream.closeEntry()
                    zipInputStream.close()
                    return file
                }
            }
        } catch (ignore: InterruptedIOException) { }
        catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }

    override fun onPostExecute(result: File?) {
        result?.let {
            listener.onSubtitleDownloaded(it)
        } ?: listener.onFailedToDownload()
    }
}