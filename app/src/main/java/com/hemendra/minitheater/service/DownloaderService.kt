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
import com.hemendra.minitheater.presenter.DownloadsPresenter

class DownloaderService: Service(), TorrentSessionListener {

    private val NOTIFICATION_ID = 1001
    private val PAUSE_ACTION = 1002
    private val STOP_ACTION = 1003
    private val NOTIFICATION_CHANNEL_ID = "MovieDownloads"

    private var localBroadcastManager: LocalBroadcastManager? = null
    private var notificationManager: NotificationManager? = null
    private var builder: NotificationCompat.Builder? = null

    companion object {
        val ACTION_STOP_DOWNLOAD = "com.hemendra.minitheater.ACTION_STOP_DOWNLOAD"

        val ACTION_PROGRESS_UPDATE = "com.hemendra.minitheater.ACTION_PROGRESS_UPDATE"
        val ACTION_DOWNLOAD_FAILED = "com.hemendra.minitheater.ACTION_DOWNLOAD_FAILED"
        val ACTION_DOWNLOAD_COMPLETE = "com.hemendra.minitheater.ACTION_DOWNLOAD_COMPLETE"
        val ACTION_DOWNLOAD_PAUSED = "com.hemendra.minitheater.ACTION_DOWNLOAD_PAUSED"
        val ACTION_DOWNLOAD_RESUMED = "com.hemendra.minitheater.ACTION_DOWNLOAD_RESUMED"
        val ACTION_DOWNLOAD_STOPPED = "com.hemendra.minitheater.ACTION_DOWNLOAD_STOPPED"

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

        localBroadcastManager = LocalBroadcastManager.getInstance(this)

        val action = intent?.getStringExtra("action")
        action?.let { act ->
            if(act == "Pause") {
                if(torrentSession?.isPaused == true) {
                    if(!resumeTorrentSession())
                        startTorrentSession()
                } else if(!pauseTorrentSession()) {
                    stopSelf()
                }
                return START_STICKY
            } else if(act == "Stop") {
                stopSelf()
            }
            return START_NOT_STICKY
        }

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val filter = IntentFilter(ACTION_STOP_DOWNLOAD)
        localBroadcastManager?.registerReceiver(commandReceiver, filter)

        if(!Utils.isNetworkAvailable(this)) {
            onDownloadFailed(TorrentFailureReason.NO_INTERNET_CONNECTION)
            return START_NOT_STICKY
        }

        movie = intent?.getSerializableExtra(EXTRA_MOVIE) as Movie?

        return if(startTorrentSession()) {
            startForeground(NOTIFICATION_ID, getNotification())
            START_STICKY
        } else {
            stopSelf()
            onDownloadFailed(TorrentFailureReason.NOTHING_TO_DOWNLOAD)
            START_NOT_STICKY
        }
    }

    private fun startTorrentSession(): Boolean {
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

            torrentSession?.stop()

            torrentSession = TorrentSession(torrentSessionOptions)
            torrentSession?.listener = this

            Thread(Runnable {
                torrentSession?.start(this, torrentUri)
            }).start()

            return true
        }
        return false
    }

    private fun pauseTorrentSession(): Boolean {
        torrentSession?.let { session ->
            session.pause()
            builder?.let { b ->
                for(a in b.mActions) {
                    if(a.title == "Pause") {
                        a.icon = R.drawable.ic_play_arrow_black_40dp
                        a.title = "Resume"
                    }
                }
                showPausedNotification()
                movie?.let {
                    it.isDownloading = true
                    it.isPaused = true
                    DownloadsPresenter.getInstance().updateDownloadProgress(it)
                }
                return true
            }
        }
        return false
    }

    private fun resumeTorrentSession(): Boolean {
        torrentSession?.let { session ->
            try {
                session.resume()
                builder?.let { b ->
                    for(a in b.mActions) {
                        if(a.title == "Resume") {
                            a.icon = R.drawable.ic_pause_black_30dp
                            a.title = "Pause"
                        }
                    }
                }
                showResumedNotification()
                movie?.let {
                    it.isDownloading = true
                    it.isPaused = false
                    DownloadsPresenter.getInstance().updateDownloadProgress(it)
                }
                return true
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }
        return false
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
        localBroadcastManager?.unregisterReceiver(commandReceiver)
        torrentSession?.stop()
        stopForeground(true)
        movie?.let { m ->
            val intent = Intent(ACTION_DOWNLOAD_PAUSED)
            intent.putExtra(EXTRA_MOVIE, m)
            m.downloadSeeds = 0
            m.downloadSpeed = 0
            m.uploadSpeed = 0
            m.isDownloading = false
            m.isPaused = false
            localBroadcastManager?.sendBroadcast(intent)
            DownloadsPresenter.getInstance().updateDownloadProgress(m)
        }
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
        builder?.addAction(getPauseAction())
        builder?.addAction(getStopAction())
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

    private fun getPauseAction(): NotificationCompat.Action {
        val intent = Intent(applicationContext, DownloaderService::class.java)
        intent.putExtra("action", "Pause")
        val pi = PendingIntent.getService(applicationContext, PAUSE_ACTION,
                intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Action(R.drawable.ic_pause_black_30dp, "Pause", pi)
    }

    private fun getStopAction(): NotificationCompat.Action {
        val intent = Intent(applicationContext, DownloaderService::class.java)
        intent.putExtra("action", "Stop")
        val pi = PendingIntent.getService(applicationContext, STOP_ACTION,
                intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Action(R.drawable.ic_stop_black_30dp, "Stop", pi)
    }

    private fun onDownloadFinished() {
        val intent = Intent(ACTION_DOWNLOAD_COMPLETE)
        intent.putExtra(EXTRA_MOVIE, movie)
        localBroadcastManager?.sendBroadcast(intent)

        movie?.let {
            it.downloadSeeds = 0
            it.uploadSpeed = 0
            it.downloadSpeed = 0
            it.downloadProgress = 100f
            it.isDownloading = false
            it.isPaused = false
            DownloadsPresenter.getInstance().updateDownloadProgress(it)
        }
    }

    private fun onDownloadFailed(reason: TorrentFailureReason) {
        val intent = Intent(ACTION_DOWNLOAD_FAILED)
        intent.putExtra(EXTRA_MOVIE, movie)
        intent.putExtra(EXTRA_FAILURE_REASON, reason)
        localBroadcastManager?.sendBroadcast(intent)

        movie?.let {
            it.downloadSeeds = 0
            it.uploadSpeed = 0
            it.downloadSpeed = 0
            it.isDownloading = false
            it.isPaused = false
            DownloadsPresenter.getInstance().updateDownloadProgress(it)
        }
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
            localBroadcastManager?.sendBroadcast(intent)

            DownloadsPresenter.getInstance().updateDownloadProgress(m)
        }
        val str = String.format(Locale.getDefault(), "%.2f%%, %d Seeds, D: %.2f KB/s, U: %.2f KB/s",
                progress * 100f, seeds, (speed.toFloat()/1024), (upSpeed.toFloat()/1024))
        notificationView?.setTextViewText(R.id.tvInfo, str)

        notificationManager?.notify(NOTIFICATION_ID, builder?.build())
    }

    private fun showPausedNotification() {
        val progress = movie?.downloadProgress ?: 0f
        val str = String.format(Locale.getDefault(), "Downloaded %.2f%% (Paused)", (progress * 100f))
        notificationView?.setTextViewText(R.id.tvInfo, str)
        notificationManager?.notify(NOTIFICATION_ID, builder?.build())

        movie?.let { m ->
            val intent = Intent(ACTION_DOWNLOAD_PAUSED)
            intent.putExtra(EXTRA_MOVIE, m)
            m.isDownloading = true
            m.isPaused = true
            localBroadcastManager?.sendBroadcast(intent)
        }
    }

    private fun showResumedNotification() {
        val speed = movie?.downloadSpeed ?: 0
        val seeds = movie?.downloadSeeds ?: 0f
        val progress = movie?.downloadProgress ?: 0f
        val upSpeed = movie?.uploadSpeed ?: 0

        val str = String.format(Locale.getDefault(), "%.2f%%, %d Seeds, D: %.2f KB/s, U: %.2f KB/s",
                progress * 100f, seeds, (speed.toFloat()/1024), (upSpeed.toFloat()/1024))

        notificationView?.setTextViewText(R.id.tvInfo, str)
        notificationManager?.notify(NOTIFICATION_ID, builder?.build())

        movie?.let { m ->
            val intent = Intent(ACTION_DOWNLOAD_RESUMED)
            m.isDownloading = true
            m.isPaused = false
            intent.putExtra(EXTRA_MOVIE, m)
            localBroadcastManager?.sendBroadcast(intent)
        }
    }

    private fun showErrorNotification() {
        notificationView?.setTextViewText(R.id.tvInfo, "Failed to Download")
        notificationManager?.notify(NOTIFICATION_ID, builder?.build())
    }

    private fun showFinishedNotification() {
        notificationView?.setTextViewText(R.id.tvInfo, "Downloaded Complete")
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
        showErrorNotification()
    }

    override fun onTorrentFinished(torrentHandle: TorrentHandle, torrentSessionStatus: TorrentSessionStatus) {
        Log.d("service", "onTorrentFinished")
        if(torrentSessionStatus.state == TorrentStatus.State.FINISHED) {
            Log.d("service", "onTorrentFinished : broadcast")
            onDownloadFinished()
            showFinishedNotification()
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