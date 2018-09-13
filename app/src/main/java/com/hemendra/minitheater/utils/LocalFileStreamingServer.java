package com.hemendra.minitheater.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * A single-connection HTTP server that will respond to requests for files and
 * pull them from the application's SD card.
 */
public class LocalFileStreamingServer implements Runnable {
    private static final String TAG = LocalFileStreamingServer.class.getName();
    private static final int port = 4848;
    private boolean isRunning = false;
    private ServerSocket socket;
    private Thread thread;
    private File mMovieFile;
    private long actualCompleteMovieLength;
    private ExternalResourceDataSource dataSource;

    private static final int DATA_READY = 1;
    private static final int DATA_NOT_READY = 2;
    private static final int DATA_CONSUMED = 3;
    private static final int DATA_NOT_AVAILABLE = 4;

    private int dataStatus = -1;

    /**
     * This server accepts HTTP request and returns files from device.
     */
    public LocalFileStreamingServer(File file, long actualCompleteMovieLength) {
        mMovieFile = file;
        this.actualCompleteMovieLength = actualCompleteMovieLength;
        dataSource = new ExternalResourceDataSource(mMovieFile);
    }

    /**
     * Prepare the server to start.
     * <p>
     * This only needs to be called once per instance. Once initialized, the
     * server can be started and stopped as needed.
     */
    public void init() {
        try {
            socket = new ServerSocket(port);
            socket.setSoTimeout(10000);
            Log.e(TAG, "Server started at localhost:"+port);
        } catch (UnknownHostException e) {
            Log.e(TAG, "Error UnknownHostException server", e);
        } catch (IOException e) {
            Log.e(TAG, "Error IOException server", e);
        }
    }

    public String getFileUrl() {
        return "http://localhost:" + port + "/" + mMovieFile.getName();
    }

    /**
     * Start the server.
     */
    public void start() {
        thread = new Thread(this);
        thread.start();
        isRunning = true;
    }

    /**
     * Stop the server.
     * <p>
     * This stops the thread listening to the port. It may take up to five
     * seconds to close the service and this call blocks until that occurs.
     */
    public void stop() {
        isRunning = false;
        if (thread == null) {
            Log.e(TAG, "Server was stopped without being started.");
            return;
        }
        Log.e(TAG, "Stopping server.");
        thread.interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Determines if the server is running (i.e. has been <code>start</code>ed
     * and has not been <code>stop</code>ed.
     *
     * @return <code>true</code> if the server is running, otherwise
     * <code>false</code>
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * This is used internally by the server and should not be called directly.
     */
    @Override
    public void run() {
        Log.e(TAG, "running");
        while (isRunning) {
            try {
                Socket client = socket.accept();
                if (client == null) {
                    continue;
                }
                Log.e(TAG, "client connected at " + port);
                Log.e(TAG, "processing request...");
                processRequest(client);
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "No client connected, waiting for client...", e);
                // Do nothing
            } catch (IOException e) {
                Log.e(TAG, "Error connecting to client", e);
                // break;
            }
        }
        Log.e(TAG, "Server interrupted or stopped. Shutting down.");
    }

    /**
     * Find byte index separating header from body. It must be the last byte of
     * the first two sequential new lines.
     **/
    private boolean headerEndReached(final byte[] buf, int totalRead) {
        for(int i=0; i+3<totalRead; i++) {
            if (buf[i] == '\r' && buf[i + 1] == '\n'
                    && buf[i + 2] == '\r' && buf[i + 3] == '\n') {
                return true;
            }
        }
        return false;
    }

    /**
     * Sends the HTTP response to the client, including headers (as applicable)
     * and content.
     */
    private void processRequest(Socket client) throws IllegalStateException, IOException {
        if (dataSource == null) {
            Log.e(TAG, "Invalid (null) resource.");
            client.close();
            return;
        }

        InputStream is = client.getInputStream();
        final int bufferSize = 8192;
        byte[] buf = new byte[bufferSize];
        int totalRead = 0;

        int read = is.read(buf, 0, bufferSize);
        while (read > 0) {
            totalRead += read;

            if(headerEndReached(buf, totalRead)) break;

            read = is.read(buf, totalRead, bufferSize - totalRead);
        }

        // Create a BufferedReader for parsing the header.
        ByteArrayInputStream bin = new ByteArrayInputStream(buf, 0, totalRead);
        BufferedReader reader = new BufferedReader(new InputStreamReader(bin));
        Properties pre = new Properties();
        Properties requestParameters = new Properties();
        Properties header = new Properties();

        decodeHeader(reader, pre, requestParameters, header);

        for (Entry<Object, Object> e : pre.entrySet()) {
            Log.d(TAG, "pre: " + e.getKey() + " : " + e.getValue());
        }

        for (Entry<Object, Object> e : requestParameters.entrySet()) {
            Log.d(TAG, "requestParameters: " + e.getKey() + " : " + e.getValue());
        }

        for (Entry<Object, Object> e : header.entrySet()) {
            Log.d(TAG, "Request Header: " + e.getKey() + " : " + e.getValue());
        }

        String range = header.getProperty("range");
        long bytesToSkip = 0;
        boolean seekRequest = false;
        if (range != null) {
            Log.d(TAG, "range is: " + range);
            seekRequest = true;
            range = range.substring(6);
            int charPos = range.indexOf('-');
            if (charPos > 0) {
                range = range.substring(0, charPos);
            }
            bytesToSkip = Long.parseLong(range);
            Log.d(TAG, "range found!! " + bytesToSkip);
        }
        String headers = "";
        // Log.e(TAG, "is seek request: " + seekRequest);
        if(dataSource.isDownloadComplete()) {
            headers += "HTTP/1.1 200 OK\r\n";
        } else {
            headers += "HTTP/1.1 206 Partial Content\r\n";
            headers += "Content-Range: bytes " + bytesToSkip + "-"
                    + dataSource.getDownloadedLength() + "/"
                    + dataSource.getContentLength(true) + "\r\n";
        }

        headers += "Content-Type: " + dataSource.getContentType() + "\r\n";
        headers += "Accept-Ranges: bytes\r\n";
        headers += "Connection: keep-alive\r\n";
        headers += "Content-Length: " + dataSource.getContentLength(true) + "\r\n";
        headers += "\r\n";

        Log.d(TAG, "Response Headers: "+headers);

        InputStream data = null;
        try {
            data = dataSource.createInputStream();
            byte[] buffer = headers.getBytes();
            Log.e(TAG, "writing to client");
            client.getOutputStream().write(buffer, 0, buffer.length);

            // Start sending content.

            byte[] buff = new byte[1024 * 50];
            Log.e(TAG, "No of bytes skipped: " + data.skip(bytesToSkip));
            int cbSentThisBatch = 0;
            while (isRunning) {
                // Check if data is ready
                while (!dataSource.isDataReady() && isRunning) {
                    if (dataStatus == DATA_READY) {
                        //Log.e(TAG, "error in reading bytes**********(Data ready)");
                        break;
                    } else if (dataStatus == DATA_CONSUMED) {
                        //Log.e(TAG, "error in reading bytes**********(All Data consumed)");
                        break;
                    } else if (dataStatus == DATA_NOT_READY) {
                        //Log.e(TAG, "error in reading bytes**********(Data not ready)");
                    } else if (dataStatus == DATA_NOT_AVAILABLE) {
                        //Log.e(TAG, "error in reading bytes**********(Data not available)");
                    }
                    // wait for a second if data is not ready
                    synchronized (this) {
                        Thread.sleep(1000);
                    }
                }
                //Log.e(TAG, "error in reading bytes**********(Data ready)");

                int cbRead = data.read(buff, 0, buff.length);
                if (cbRead == -1 && isRunning) {
                    Log.e(TAG,
                            "read-bytes are -1 and this is simulate streaming, close the ips and create another");
                    data.close();
                    data = dataSource.createInputStream();
                    cbRead = data.read(buff, 0, buff.length);
                    if (cbRead == -1 && isRunning) {
                        Log.e(TAG, "error in reading bytes**********");
                        throw new IOException(
                                "Error re-opening data source for looping.");
                    }
                }

                if(isRunning) {
                    client.getOutputStream().write(buff, 0, cbRead);
                    client.getOutputStream().flush();
                    bytesToSkip += cbRead;
                    cbSentThisBatch += cbRead;

                    dataSource.streamedBytes += cbRead;
                }
            }
            Log.e(TAG, "cbSentThisBatch: " + cbSentThisBatch);
            // If we did nothing this batch, block for a second
            if (cbSentThisBatch == 0 && isRunning) {
                Log.e(TAG, "Blocking until more data appears");
                Thread.sleep(1000);
            }
        } catch (SocketException e) {
            // Ignore when the client breaks connection
            Log.e(TAG, "Ignoring " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "Error getting content stream.", e);
        } catch (Exception e) {
            Log.e(TAG, "Error streaming file content.", e);
        } finally {
            if (data != null) {
                data.close();
            }
            client.close();
        }
    }

    /**
     * Decodes the sent headers and loads the data into java Properties' key -
     * value pairs
     **/
    private void decodeHeader(BufferedReader in, Properties pre,
                              Properties parms, Properties header) {
        try {
            // Read the request line
            String inLine = in.readLine();
            if (inLine == null)
                return;
            StringTokenizer st = new StringTokenizer(inLine);
            if (!st.hasMoreTokens())
                Log.e(TAG,
                        "BAD REQUEST: Syntax error. Usage: GET /example/file.html");

            String method = st.nextToken();
            pre.put("method", method);

            if (!st.hasMoreTokens())
                Log.e(TAG,
                        "BAD REQUEST: Missing URI. Usage: GET /example/file.html");

            String uri = st.nextToken();

            // Decode parameters from the URI
            int qmi = uri.indexOf('?');
            if (qmi >= 0) {
                decodeParms(uri.substring(qmi + 1), parms);
                uri = decodePercent(uri.substring(0, qmi));
            } else
                uri = decodePercent(uri);

            // If there's another token, it's protocol version,
            // followed by HTTP headers. Ignore version but parse headers.
            // NOTE: this now forces header names lowercase since they are
            // case insensitive and vary by client.
            if (st.hasMoreTokens()) {
                String line = in.readLine();
                while (line != null && line.trim().length() > 0) {
                    int p = line.indexOf(':');
                    if (p >= 0)
                        header.put(line.substring(0, p).trim().toLowerCase(),
                                line.substring(p + 1).trim());
                    line = in.readLine();
                }
            }

            pre.put("uri", uri);
        } catch (IOException ioe) {
            Log.e(TAG,
                    "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
        }
    }

    /**
     * Decodes parameters in percent-encoded URI-format ( e.g.
     * "name=Jack%20Daniels&pass=Single%20Malt" ) and adds them to given
     * Properties. NOTE: this doesn't support multiple identical keys due to the
     * simplicity of Properties -- if you need multiples, you might want to
     * replace the Properties with a Hashtable of Vectors or such.
     */
    private void decodeParms(String parms, Properties p) {
        if (parms == null)
            return;

        StringTokenizer st = new StringTokenizer(parms, "&");
        while (st.hasMoreTokens())
        {
            String e = st.nextToken();
            int sep = e.indexOf('=');
            if (sep >= 0)
                p.put(decodePercent(e.substring(0, sep)).trim(),
                        decodePercent(e.substring(sep + 1)));
        }
    }

    /**
     * Decodes the percent encoding scheme. <br/>
     * For example: "an+example%20string" -> "an example string"
     */
    private String decodePercent(String str) {
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                switch (c) {
                    case '+':
                        sb.append(' ');
                        break;
                    case '%':
                        sb.append((char) Integer.parseInt(
                                str.substring(i + 1, i + 3), 16));
                        i += 2;
                        break;
                    default:
                        sb.append(c);
                        break;
                }
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "BAD REQUEST: Bad percent-encoding.");
            return null;
        }
    }

    public float getBufferPercentage() {
        return dataSource.getDownloadedPercentage();
    }

    /**
     * provides meta-data and access to a stream for resources on SD card.
     */
    protected class ExternalResourceDataSource {
        private final File movieResource;
        long contentLength;
        private FileInputStream inputStream = null;
        long streamedBytes = 0;

        ExternalResourceDataSource(File resource) {
            movieResource = resource;
            Log.e(TAG, "movieResource path is: " + movieResource.getPath());
        }

        /**
         * Returns a MIME-compatible content type (e.g. "text/html") for the
         * resource. This method must be implemented.
         *
         * @return A MIME content type.
         */
        String getContentType() {
            return "video/mp4";
        }

        /**
         * Creates and opens an input stream that returns the contents of the
         * resource. This method must be implemented.
         *
         * @return An <code>InputStream</code> to access the resource.
         */
        InputStream createInputStream() {
            // NB: Because createInputStream can only be called once per asset
            // we always create a new file descriptor here.
            try {
                inputStream = new FileInputStream(movieResource);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            // File temp = new File(resourcePath);
            contentLength = movieResource.length();
            Log.e(TAG, "file exists??" + movieResource.exists()
                    + " and content length is: " + contentLength);

            return inputStream;
        }

        /**
         * Returns the length of resource in bytes.
         * <p>
         * By default this returns -1, which causes no content-type header to be
         * sent to the client. This would make sense for a stream content of
         * unknown or undefined length. If your resource has a defined length
         * you should override this method and return that.
         *
         * @return The length of the resource in bytes.
         */
        long getContentLength(boolean ignoreSimulation) {
            if (!ignoreSimulation) {
                return -1;
            }
            return actualCompleteMovieLength;
        }

        long getDownloadedLength() {
            return movieResource.length();
        }

        boolean isDownloadComplete() {
            return getDownloadedLength() >= actualCompleteMovieLength;
        }

        float getDownloadedPercentage() {
            double downloaded = (double) getDownloadedLength();
            double total = (double) actualCompleteMovieLength;
            float percent = (float)((downloaded / total) * 100f);
            return percent > 100f ? 100f : percent;
        }

        boolean isDataReady() {
            dataStatus = -1;
            boolean res = false;
            long downloadedBytes = getDownloadedLength();
            /*Log.d(TAG, String.format("Downloaded: %.3f MB / %.3f MB",
                    ((double)downloadedBytes/1024f/1024f),
                    ((double)actualCompleteMovieLength/1024f/1024f)));*/
            if(actualCompleteMovieLength == downloadedBytes){
                dataStatus = DATA_CONSUMED;
                res = false;
            }else if(downloadedBytes > streamedBytes){
                dataStatus = DATA_READY;
                res = true;
            }else if(downloadedBytes <= streamedBytes){
                dataStatus = DATA_NOT_READY;
                res = false;
            }else if(actualCompleteMovieLength == -1){
                dataStatus = DATA_NOT_AVAILABLE;
                res = false;
            }
            return res;
        }

    }
}