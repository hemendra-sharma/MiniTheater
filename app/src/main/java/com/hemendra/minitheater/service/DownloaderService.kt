package com.hemendra.minitheater.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.RemoteViews
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.jlibtorrent.TorrentStatus
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.utils.RemoteConfig
import com.hemendra.minitheater.utils.Utils
import com.hemendra.minitheater.view.MainActivity
import com.masterwok.simpletorrentandroid.TorrentSession
import com.masterwok.simpletorrentandroid.TorrentSessionOptions
import com.masterwok.simpletorrentandroid.contracts.TorrentSessionListener
import com.masterwok.simpletorrentandroid.models.TorrentSessionStatus
import java.io.File
import java.util.*
import android.app.NotificationChannel
import android.app.NotificationManager
import android.annotation.TargetApi
import android.appwidget.AppWidgetManager
import android.os.Build

class DownloaderService: Service(), TorrentSessionListener {

    private val NOTIFICATION_ID = 1001
    private val NOTIFICATION_CHANNEL_ID = "MovieDownloads"

    private val localBroadcastManager = LocalBroadcastManager.getInstance(this)
    private var notificationManager: NotificationManager? = null
    private var builder: NotificationCompat.Builder? = null

    companion object {
        val ACTION_STOP_DOWNLOAD = "com.hemendra.minitheater.ACTION_STOP_DOWNLOAD"

        val ACTION_PROGRESS_UPDATE = "com.hemendra.minitheater.ACTION_PROGRESS_UPDATE"
        val ACTION_DOWNLOAD_FAILED = "com.hemendra.minitheater.ACTION_DOWNLOAD_FAILED"
        val ACTION_DOWNLOAD_COMPLETE = "com.hemendra.minitheater.ACTION_DOWNLOAD_COMPLETE"

        val EXTRA_FAILURE_REASON = "reason"
        val EXTRA_MOVIE = "movie"

        var isRunning = false
    }

    private var movie: Movie? = null
    private var torrentSession: TorrentSession? = null
    private var notificationView: RemoteViews? = null

    private val downloadsDirectory: File
    init {
        val path = """${Environment.getExternalStorageDirectory().absolutePath}/MiniTheater"""
        downloadsDirectory = File(path)
    }

    override fun onCreate() {
        isRunning = true
        super.onCreate()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val filter = IntentFilter(ACTION_STOP_DOWNLOAD)
        localBroadcastManager.registerReceiver(commandReceiver, filter)

        if(!Utils.isNetworkAvailable(this)) {
            onDownloadFailed(TorrentFailureReason.NO_INTERNET_CONNECTION)
            return START_NOT_STICKY
        }

        movie = intent?.getSerializableExtra(EXTRA_MOVIE) as Movie?

        movie?.let {
            val savingDirectory = getDownloadDirectoryFor(it.torrents[0])
            Log.d("service", "savingDirectory: ${savingDirectory.absolutePath}")
            val url = RemoteConfig.getInstance().getMagnetURL(it)
            Log.d("service", "url: $url")
            val torrentUri = Uri.parse(url)
            val torrentSessionOptions = TorrentSessionOptions(
                    downloadLocation = savingDirectory
                    , onlyDownloadLargestFile = true
                    , enableLogging = false
                    , shouldStream = true)

            torrentSession = TorrentSession(torrentSessionOptions)
            torrentSession?.listener = this

            Thread(Runnable {
                torrentSession?.start(this, torrentUri)
            }).start()

            startForeground(NOTIFICATION_ID, getNotification())

            return START_STICKY
        } ?: stopSelf()

        onDownloadFailed(TorrentFailureReason.NOTHING_TO_DOWNLOAD)
        return START_NOT_STICKY
    }

    private fun getDownloadDirectoryFor(torrent: com.hemendra.minitheater.data.Torrent): File {
        val path = """${downloadsDirectory.absolutePath}/${torrent.hash}"""
        return File(path)
    }

    private val commandReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if(intent?.action == ACTION_STOP_DOWNLOAD) {
                onDownloadFailed(TorrentFailureReason.ABORTED)
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        localBroadcastManager.unregisterReceiver(commandReceiver)
        torrentSession?.pause()
        stopForeground(true)
        super.onDestroy()
        isRunning = false
    }

    private fun getNotification(): Notification? {
        builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        builder?.setSmallIcon(R.mipmap.ic_launcher_foreground)
        builder?.setTicker("Downloading ${movie?.title ?: ""}")
        builder?.setOngoing(true)
        builder?.setContentTitle(movie?.title ?: "Downloading Movie")
        builder?.setContentText("Downloading")
        builder?.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        builder?.setSound(null)
        notificationView = RemoteViews(packageName, R.layout.foreground_service_notification_view)
        notificationView?.setOnClickPendingIntent(R.id.rlContainer, getPendingIntent())
        builder?.setCustomContentView(notificationView)
        movie?.let { m ->
            notificationView?.setTextViewText(R.id.tvTitle, m.title)
            notificationView?.setTextViewText(R.id.tvInfo, "Starting Download...")
        }
        val notification = builder?.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createChannel()
        return notification
    }

    @TargetApi(26)
    private fun createChannel() {
        val name = NOTIFICATION_CHANNEL_ID
        val description = "Notifications for Download Status"
        val importance = NotificationManager.IMPORTANCE_LOW

        val mChannel = NotificationChannel(name, name, importance)
        mChannel.description = description
        mChannel.enableLights(false)
        mChannel.setSound(null, null)

        notificationManager?.createNotificationChannel(mChannel)
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.putExtra("from_downloading_notification", true)
        return PendingIntent.getActivity(applicationContext, NOTIFICATION_ID,
                intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun onDownloadFinished() {
        val intent = Intent(ACTION_DOWNLOAD_COMPLETE)
        intent.putExtra(EXTRA_MOVIE, movie)
        localBroadcastManager.sendBroadcast(intent)
    }

    private fun onDownloadFailed(reason: TorrentFailureReason) {
        val intent = Intent(ACTION_DOWNLOAD_FAILED)
        intent.putExtra(EXTRA_MOVIE, movie)
        intent.putExtra(EXTRA_FAILURE_REASON, reason)
        localBroadcastManager.sendBroadcast(intent)
    }

    private fun publishProgress(torrentSessionStatus: TorrentSessionStatus) {
        val speed = torrentSessionStatus.downloadRate
        val seeds = torrentSessionStatus.seederCount
        val progress = torrentSessionStatus.progress
        val upSpeed = torrentSessionStatus.uploadRate

        Log.d("service", "publishProgress | speed: $speed, seeds: $seeds, progress: $progress")
        Log.d("service", "publishProgress | downloaded: ${torrentSessionStatus.bytesDownloaded}," +
                " wanted: ${torrentSessionStatus.bytesWanted}")

        movie?.let { m ->
            m.downloadProgress = progress
            m.downloadSeeds = seeds
            m.downloadSpeed = speed
            m.uploadSpeed = upSpeed

            val intent = Intent(ACTION_PROGRESS_UPDATE)
            intent.putExtra(EXTRA_MOVIE, m)
            localBroadcastManager.sendBroadcast(intent)
        }
        val str = String.format(Locale.getDefault(), "%.2f%%, %d Seeds, %.2f KB/s",
                progress * 100f, seeds, (speed.toFloat()/1024))
        notificationView?.setTextViewText(R.id.tvInfo, str)

        notificationManager?.notify(NOTIFICATION_ID, builder?.build())
    }

    override fun onAddTorrent(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
        Log.d("service", "onAddTorrent")
    }

    override fun onBlockUploaded(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
        Log.d("service", "onBlockUploaded")
    }

    override fun onMetadataFailed(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
        Log.d("service", "onMetadataFailed")
    }

    override fun onMetadataReceived(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
        Log.d("service", "onMetadataReceived")
    }

    override fun onPieceFinished(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
        Log.d("service", "onPieceFinished")
        publishProgress(torrentSessionStatus)
    }

    override fun onTorrentDeleteFailed(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
        Log.d("service", "onTorrentDeleteFailed")
    }

    override fun onTorrentDeleted(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
        Log.d("service", "onTorrentDeleted")
    }

    override fun onTorrentError(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
        Log.d("service", "onTorrentError")
        onDownloadFailed(TorrentFailureReason.UNKNOWN)
    }

    override fun onTorrentFinished(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
        Log.d("service", "onTorrentFinished")
        if(torrentSessionStatus.state == TorrentStatus.State.FINISHED) {
            Log.d("service", "onTorrentFinished : broadcast")
            onDownloadFinished()
        }
    }

    override fun onTorrentPaused(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
        Log.d("service", "onTorrentPaused")
    }

    override fun onTorrentRemoved(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
        Log.d("service", "onTorrentRemoved")
    }

    override fun onTorrentResumed(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
        Log.d("service", "onTorrentResumed")
    }
}