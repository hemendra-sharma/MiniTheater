package com.hemendra.minitheater.data.model.images

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import com.hemendra.minitheater.data.listeners.IImageLoaderListener
import com.hemendra.minitheater.utils.ConnectionCallback
import com.hemendra.minitheater.utils.ContentDownloader
import com.hemendra.minitheater.utils.CustomAsyncTask
import com.hemendra.minitheater.view.listeners.ImageLoaderCallback
import java.net.HttpURLConnection

class ImageLoader(private var db: ImagesDB,
                  private var url: String,
                  private var callback: ImageLoaderCallback,
                  private var listener: IImageLoaderListener) :
        CustomAsyncTask<Void, Void, Bitmap?>() {

    private var connection: HttpURLConnection? = null
    val createdAt = System.currentTimeMillis()

    private var disconnectHandler: Handler? = null

    private val disconnectCallback: Handler.Callback = Handler.Callback {
        connection?.disconnect()
        true
    }

    override fun cancel(interrupt: Boolean) {
        disconnectHandler?.sendEmptyMessage(0)
        super.cancel(true)
    }

    override fun doInBackground(vararg params: Void): Bitmap? {
        if(Looper.myLooper() == null) Looper.prepare()

        var arr = db.getImage(url)
        arr?.let {
            return BitmapFactory.decodeByteArray(it, 0, it.size)
        }
        arr = ContentDownloader.getByteArray(url, object: ConnectionCallback{
            override fun onConnectionInitialized(conn: HttpURLConnection) {
                connection = conn
                disconnectHandler = Handler(disconnectCallback)
            }
            override fun onResponseCode(code: Int){}
            override fun onInterrupted(){}
            override fun onError(){}

        })
        arr?.let {
            db.insertImage(url, it)
            return BitmapFactory.decodeByteArray(it, 0, it.size)
        }
        return null
    }

    override fun onPostExecute(result: Bitmap?) {
        if(result != null) callback.onImageLoaded(url, result)
        listener.onExecutionFinished()
    }

}