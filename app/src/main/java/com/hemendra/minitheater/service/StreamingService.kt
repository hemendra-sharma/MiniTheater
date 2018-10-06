package com.hemendra.minitheater.service

import android.annotation.TargetApi
import android.app.*
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.LocalBroadcastManager
import android.widget.RemoteViews
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.presenter.DownloadsPresenter
import com.hemendra.minitheater.view.MainActivity

class StreamingService: Service() {

    companion object {
        const val EXTRA_MOVIE = "movie"

        private const val NOTIFICATION_ID = 3001
        private const val NOTIFICATION_CHANNEL_ID = "MovieDownloads"

        const val STOP_ACTION = 3003

        var isRunning = false
        var movie: Movie? = null

        const val ACTION_STREAM_STARTED = "com.hemendra.minitheater.ACTION_STREAM_STARTED"
        const val ACTION_STREAM_STOPPED = "com.hemendra.minitheater.ACTION_STREAM_STOPPED"
    }

    private var localBroadcastManager: LocalBroadcastManager? = null

    private var notificationManager: NotificationManager? = null
    private var builder: NotificationCompat.Builder? = null
    private var notificationView: RemoteViews? = null

    private var streamingServer: StreamingServer? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        isRunning = true
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val action = intent?.getStringExtra("action")
        action?.let { act ->
            if(act == "Stop") {
                notificationManager?.cancel(NOTIFICATION_ID)
                stopSelf()
            }
            return START_NOT_STICKY
        }

        movie = intent?.getSerializableExtra(EXTRA_MOVIE) as Movie?

        movie?.let {
            val file = DownloadsPresenter.getInstance().getTorrentFile(it.torrents[0])
            if(file != null) {
                streamingServer = StreamingServer(it, streamingServerListener)
                streamingServer?.start()
                startForeground(NOTIFICATION_ID, getNotification())

                val i = Intent(ACTION_STREAM_STARTED)
                i.putExtra(EXTRA_MOVIE, it)
                localBroadcastManager = LocalBroadcastManager.getInstance(this)
                localBroadcastManager?.sendBroadcast(i)

                return START_NOT_STICKY
            }
        }

        stopSelf()
        return START_NOT_STICKY
    }

    private val streamingServerListener = object : StreamingServerListener {
        override fun started(address: String) {
            updateNotification("Server: $address")
        }
        override fun failedToStart() {
            stopForeground(true)
            stopSelf()
        }
    }

    override fun onDestroy() {
        streamingServer?.requestStop()
        stopForeground(true)

        movie?.let {
            val i = Intent(ACTION_STREAM_STOPPED)
            i.putExtra(EXTRA_MOVIE, it)
            localBroadcastManager?.sendBroadcast(i)
            it.isStreaming = false
            DownloadsPresenter.getInstance().updateMovie(it)
        }

        movie = null
        isRunning = false
        super.onDestroy()
    }

    private fun getNotification(): Notification? {
        builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        builder?.setSmallIcon(R.drawable.ic_streaming_black_40dp)
        builder?.setLargeIcon(BitmapFactory.decodeResource(resources,
                R.mipmap.ic_launcher_foreground))
        builder?.setTicker("Streaming ${movie?.title ?: "Movie"}")
        builder?.setOngoing(true)
        builder?.setContentTitle("Streaming ${movie?.title ?: "Movie"}")
        builder?.setContentText("Streaming")
        builder?.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        builder?.setSound(null)
        builder?.addAction(getStopAction())
        notificationView = RemoteViews(packageName, R.layout.foreground_service_notification_view)
        notificationView?.setOnClickPendingIntent(R.id.rlContainer, getPendingIntent())
        builder?.setCustomContentView(notificationView)
        movie?.let { m ->
            notificationView?.setTextViewText(R.id.tvTitle, "Streaming ${m.title}")
            notificationView?.setTextViewText(R.id.tvInfo, "Starting Server...")
        }

        val notification = builder?.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createChannel()
        return notification
    }

    @TargetApi(26)
    private fun createChannel() {
        val name = NOTIFICATION_CHANNEL_ID
        val description = "Notifications for Streaming Info"
        val importance = NotificationManager.IMPORTANCE_LOW

        val mChannel = NotificationChannel(name, name, importance)
        mChannel.description = description
        mChannel.enableLights(false)
        mChannel.setSound(null, null)

        notificationManager?.createNotificationChannel(mChannel)
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java)
        return PendingIntent.getActivity(applicationContext, NOTIFICATION_ID,
                intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun getStopAction(): NotificationCompat.Action {
        val intent = Intent(applicationContext, StreamingService::class.java)
        intent.putExtra("action", "Stop")
        val pi = PendingIntent.getService(applicationContext, STOP_ACTION,
                intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Action(R.drawable.ic_stop_black_30dp, "Stop", pi)
    }

    private fun updateNotification(info: String) {
        notificationView?.setTextViewText(R.id.tvInfo, info)
        builder?.build()?.let {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, it)
        }
    }
}