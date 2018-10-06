package com.hemendra.minitheater.service

import android.os.Handler
import android.util.Log
import android.webkit.MimeTypeMap
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.presenter.DownloadsPresenter
import com.hemendra.minitheater.utils.Utils
import java.io.*
import java.lang.NullPointerException
import java.net.*
import java.util.*

class StreamingServer(private val movie: Movie,
                      private var listener: StreamingServerListener): Thread() {

    companion object {
        private const val TAG = "StreamingServer"

        private const val STATUS_FAILED = 0
        private const val STATUS_STARTED = 1
    }

    private var serverSocket: ServerSocket? = null

    private val movieFile: File = DownloadsPresenter.
            getInstance().getTorrentFile(movie.torrents[0])!!

    private val portNumber = 4848
    private var keepRunning = false

    private var isRunning = false

    private val handler = Handler(Handler.Callback { msg ->
        when(msg.what) {
            STATUS_FAILED -> listener.failedToStart()
            STATUS_STARTED -> listener.started("http://${Utils.getLocalIpAddress()}:$portNumber")
        }
        true
    })

    override fun start() {
        keepRunning = true
        super.start()
    }

    fun requestStop() {
        keepRunning = false
        interrupt()
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun run() {
        isRunning = true


        // setup the socket
        try {
            serverSocket = ServerSocket()
            serverSocket?.reuseAddress = true
            serverSocket?.soTimeout = 300000
            serverSocket?.bind(InetSocketAddress(portNumber))
            handler.sendEmptyMessage(STATUS_STARTED)
        }catch (e: IOException) {
            e.printStackTrace()
            handler.sendEmptyMessage(STATUS_FAILED) // failed to start
            keepRunning = false
        }catch (e: InterruptedException) {
            e.printStackTrace()
            handler.sendEmptyMessage(STATUS_FAILED) // interrupted
            keepRunning = false
        }

        // start listening
        while(keepRunning) {
            try {
                val client = serverSocket?.accept()
                client?.let {
                    Thread { processRequestForClient(it) }.start()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: SocketTimeoutException) {
                e.printStackTrace()
            }
        }

        isRunning = false
    }

    private fun processRequestForClient(client: Socket) {
        val fileInputStream = FileInputStream(movieFile)
        try {
            val uriAndBytesToSkip = handleHeaders(client)
            if(uriAndBytesToSkip?.bytesToSkip ?: -1 < 0)
                return

            if(uriAndBytesToSkip?.bytesToSkip ?: 0 > 0) {
                val skipped = fileInputStream.skip(uriAndBytesToSkip?.bytesToSkip ?: 0)
                Log.d(TAG, "No of bytes skipped: $skipped")
            }
            val buff = ByteArray(1024 * 50)

            if(uriAndBytesToSkip?.uri == "movie") {
                val movieObjectData = Utils.getSerializedData(movie)
                val bin = ByteArrayInputStream(movieObjectData)
                while (keepRunning && !client.isClosed && client.isConnected) {
                    val cbRead = bin.read(buff)
                    if (cbRead >= 0) {
                        client.getOutputStream().write(buff, 0, cbRead)
                        client.getOutputStream().flush()
                    } else {
                        break
                    }
                }
            } else {
                while (keepRunning && !client.isClosed && client.isConnected) {
                    val cbRead = fileInputStream.read(buff)
                    if (cbRead >= 0) {
                        client.getOutputStream().write(buff, 0, cbRead)
                        client.getOutputStream().flush()
                    } else {
                        break
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: SocketException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        } finally {
            fileInputStream.close()
            client.close()
        }
    }

    internal class UriAndBytesToSkip(val uri: String, val bytesToSkip: Long)

    private fun handleHeaders(client: Socket): UriAndBytesToSkip? {
        try {
            val inputStream = client.getInputStream()
            val buf = ByteArray(1024)
            val outArr = ByteArrayOutputStream()
            var read = inputStream.read(buf)
            while (read > 0) {
                outArr.write(buf, 0, read)
                if (headerEndReached(outArr.toByteArray())) break
                read = inputStream.read(buf)
            }

            // Create a BufferedReader for parsing the header.
            val reader = BufferedReader(StringReader(String(outArr.toByteArray())))
            val pre = Properties()
            val requestParameters = Properties()
            val header = Properties()

            decodeHeader(reader, pre, requestParameters, header)

            var uri = "stream"

            for ((key, value) in pre) {
                Log.d(TAG, "pre: $key : $value")
                if(key == "uri") {
                    uri = (value as String).replace("/", "")
                    if(uri != "movie" && uri != "stream") uri = "stream"
                }
            }

            for ((key, value) in requestParameters)
                Log.d(TAG, "requestParameters: $key : $value")
            for ((key, value) in header)
                Log.d(TAG, "Request Header: $key : $value")

            var range: String? = header.getProperty("range")
            var bytesToSkip: Long = 0

            if (range != null) {
                Log.d(TAG, "range is: $range")

                range = range.substring(6)
                val charPos = range.indexOf('-')
                if (charPos > 0) {
                    range = range.substring(0, charPos)
                }
                bytesToSkip = java.lang.Long.parseLong(range)
                Log.d(TAG, "range found!! $bytesToSkip")
            }

            var headers = ""

            val movieObjectData = Utils.getSerializedData(movie)

            if(bytesToSkip > 0) {
                headers += "HTTP/1.1 206 Partial Content\r\n"
                headers += if(uri == "movie") {
                    ("Content-Range: bytes " + bytesToSkip + "-" + bytesToSkip + "/"
                            + movieFile.length() + "\r\n")
                } else {
                    ("Content-Range: bytes " + bytesToSkip + "-" + bytesToSkip + "/"
                            + movieObjectData?.size + "\r\n")
                }
            } else {
                headers += "HTTP/1.1 200 OK\r\n"
            }
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(movieFile.extension)
            headers += if(uri == "movie") {
                "Content-Type: application/octet-stream\r\n"
            } else {
                "Content-Type: $mimeType\r\n"
            }
            headers += "Accept-Ranges: bytes\r\n"
            headers += "Connection: keep-alive\r\n"
            headers += "Access-Control-Allow-Origin: *\r\n"
            headers += "Access-Control-Expose-Headers: origin, range\r\n"
            headers += if(uri == "movie") {
                "Content-Length: " + movieObjectData?.size + "\r\n"
            } else {
                "Content-Length: " + movieFile.length() + "\r\n"
            }
            headers += "\r\n"

            Log.d(TAG, "Response Headers: $headers")

            val buffer = headers.toByteArray()
            Log.d(TAG, "writing to client")
            client.getOutputStream().write(buffer, 0, buffer.size)

            return UriAndBytesToSkip(uri, bytesToSkip)
        } catch (e: IOException) {
            e.printStackTrace()
            client.close()
        } catch (e: SocketException) {
            e.printStackTrace()
            client.close()
        }
        return null
    }

    /**
     * Find byte index separating header from body. It must be the last byte of
     * the first two sequential new lines.
     */
    private fun headerEndReached(buf: ByteArray): Boolean {
        var i = 0
        while (i + 3 < buf.size) {
            if (buf[i] == '\r'.toByte() && buf[i + 1] == '\n'.toByte()
                    && buf[i + 2] == '\r'.toByte() && buf[i + 3] == '\n'.toByte()) {
                return true
            }
            i++
        }
        return false
    }

    /**
     * Decodes the sent headers and loads the data into java Properties' key -
     * value pairs
     */
    private fun decodeHeader(reader: BufferedReader, pre: Properties,
                             parms: Properties, header: Properties) {
        try {
            // Read the request line
            val inLine = reader.readLine() ?: return
            val st = StringTokenizer(inLine)
            if (!st.hasMoreTokens())
                Log.e(TAG,
                        "BAD REQUEST: Syntax error. Usage: GET /example/file.html")

            val method = st.nextToken()
            pre["method"] = method

            if (!st.hasMoreTokens())
                Log.e(TAG,
                        "BAD REQUEST: Missing URI. Usage: GET /example/file.html")

            var uri: String? = st.nextToken()

            // Decode parameters from the URI
            val qmi = uri!!.indexOf('?')
            uri = if (qmi >= 0) {
                decodeParms(uri.substring(qmi + 1), parms)
                decodePercent(uri.substring(0, qmi))
            } else
                decodePercent(uri)

            // If there's another token, it's protocol version,
            // followed by HTTP headers. Ignore version but parse headers.
            // NOTE: this now forces header names lowercase since they are
            // case insensitive and vary by client.
            if (st.hasMoreTokens()) {
                var line: String? = reader.readLine()
                while (line != null && line.trim { it <= ' ' }.isNotEmpty()) {
                    val p = line.indexOf(':')
                    if (p >= 0)
                        header[line.substring(0, p).trim { it <= ' ' }.toLowerCase()] = line.substring(p + 1).trim { it <= ' ' }
                    line = reader.readLine()
                }
            }

            pre["uri"] = uri
        } catch (ioe: IOException) {
            Log.e(TAG,
                    "SERVER INTERNAL ERROR: IOException: " + ioe.message)
        }

    }

    /**
     * Decodes parameters in percent-encoded URI-format ( e.g.
     * "name=Jack%20Daniels&pass=Single%20Malt" ) and adds them to given
     * Properties. NOTE: this doesn't support multiple identical keys due to the
     * simplicity of Properties -- if you need multiples, you might want to
     * replace the Properties with a Hashtable of Vectors or such.
     */
    private fun decodeParms(parms: String, p: Properties) {
        val st = StringTokenizer(parms, "&")
        while (st.hasMoreTokens()) {
            val e = st.nextToken()
            val sep = e.indexOf('=')
            if (sep >= 0)
                p[decodePercent(e.substring(0, sep))!!.trim { it <= ' ' }] = decodePercent(e.substring(sep + 1))!!
        }
    }

    /**
     * Decodes the percent encoding scheme. <br></br>
     * For example: "an+example%20string" -> "an example string"
     */
    private fun decodePercent(str: String): String? {
        try {
            val sb = StringBuilder()
            var i = 0
            while (i < str.length) {
                val c = str[i]
                when (c) {
                    '+' -> sb.append(' ')
                    '%' -> {
                        sb.append(Integer.parseInt(
                                str.substring(i + 1, i + 3), 16).toChar())
                        i += 2
                    }
                    else -> sb.append(c)
                }
                i++
            }
            return sb.toString()
        } catch (e: Exception) {
            Log.e(TAG, "BAD REQUEST: Bad percent-encoding.")
            return null
        }

    }

}