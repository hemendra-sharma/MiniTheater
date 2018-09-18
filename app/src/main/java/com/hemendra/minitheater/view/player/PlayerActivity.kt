package com.hemendra.minitheater.view.player

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.Toast
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.MovieObjectType
import com.hemendra.minitheater.data.Subtitle
import com.hemendra.minitheater.presenter.DownloadsPresenter
import com.hemendra.minitheater.presenter.SubtitlesPresenter
import com.hemendra.minitheater.presenter.listeners.SubtitleDownloadListener
import com.hemendra.minitheater.presenter.listeners.SubtitlesListDownloadListener
import com.hemendra.minitheater.service.DownloaderService
import com.hemendra.minitheater.utils.LocalFileStreamingServer
import com.hemendra.minitheater.utils.Utils
import com.hemendra.minitheater.view.showMessage
import kotlinx.android.synthetic.main.activity_player.*
import java.io.File
import java.io.IOException
import java.util.*

class PlayerActivity : AppCompatActivity(), SurfaceHolder.Callback,
        MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnVideoSizeChangedListener,
        MediaPlayer.OnErrorListener, MediaController.MediaPlayerControl,
        MediaPlayer.OnInfoListener, View.OnTouchListener {

    companion object {
        private const val TAG = "PlayerActivity"
    }

    private var localFileStreamingServer: LocalFileStreamingServer? = null

    private var mediaPlayer: MediaPlayer? = null
    private var mediaController: MediaController? = null

    private var isVideoReadyToBePlayed = false
    private var movie: Movie? = null

    private val handler = Handler()

    private var mediaPlayerPrepared = false

    private var clickDetector: GestureDetector? = null

    private var positionBeforePaused = 0

    private var activityShowing = false

    private var subtitlesAdapter: SubtitlesListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        movie = intent.getSerializableExtra("movie") as Movie

        surfaceView.holder.addCallback(this)
        llHolder.setOnTouchListener(this)
        if(movie?.movieObjectType == MovieObjectType.DEFAULT) {
            ivSubtitles.setOnClickListener { _ ->
                if (rlSubtitles.visibility == View.VISIBLE) {
                    rlSubtitles.visibility = View.GONE
                } else {
                    rlSubtitles.visibility = View.VISIBLE
                    mediaController?.hide()
                    tvDownloadInfo.visibility = View.GONE
                    if (pbSubtitles.visibility == View.VISIBLE) {
                        movie?.let {
                            SubtitlesPresenter.getInstance().getSubtitlesList(it,
                                    object : SubtitlesListDownloadListener {
                                        override fun onListDownloaded(subtitlesList: ArrayList<Subtitle>) {
                                            subtitlesAdapter =
                                                    SubtitlesListAdapter(applicationContext, subtitlesList)
                                            lvSubtitles.adapter = subtitlesAdapter
                                            pbSubtitles.visibility = View.GONE
                                        }

                                        override fun onFailedToDownloadList() {
                                            pbSubtitles.visibility = View.VISIBLE
                                        }
                                    })
                        }
                    }
                }
            }
        } else {
            ivSubtitles.visibility = View.GONE
        }

        lvSubtitles.onItemClickListener = listItemClickListener

        clickDetector = GestureDetector(this,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapUp(e: MotionEvent): Boolean {
                        if (mediaController?.isShowing == true) {
                            hideSystemUI()
                            mediaController?.hide()
                            if(movie?.movieObjectType == MovieObjectType.DEFAULT)
                                ivSubtitles.visibility = View.GONE
                            tvDownloadInfo.visibility = View.GONE
                        } else {
                            showSystemUI()
                            mediaController?.show(0)
                            if(movie?.movieObjectType == MovieObjectType.DEFAULT)
                                ivSubtitles.visibility = View.VISIBLE
                            tvDownloadInfo.visibility = View.VISIBLE
                        }
                        rlSubtitles.visibility = View.GONE
                        return true
                    }
                })

        showSystemUI()
    }

    override fun onResume() {
        super.onResume()
        activityShowing = true
        val mgr = LocalBroadcastManager.getInstance(applicationContext)
        val filter = IntentFilter(DownloaderService.ACTION_PROGRESS_UPDATE)
        mgr.registerReceiver(downloadReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        activityShowing = false
        mediaPlayer?.pause()
        val mgr = LocalBroadcastManager.getInstance(applicationContext)
        mgr.unregisterReceiver(downloadReceiver)
    }

    private val listItemClickListener = AdapterView.OnItemClickListener {
        _, _, position, _ ->
        subtitlesAdapter?.let {
            rlSubtitles.visibility = View.GONE
            ivSubtitles.visibility = View.GONE
            val subtitle = it.getItem(position)
            Toast.makeText(applicationContext, "Downloading Subtitles...",
                    Toast.LENGTH_SHORT).show()
            SubtitlesPresenter.getInstance().getSubtitle(subtitle.path,
                    object: SubtitleDownloadListener{
                        override fun onSubtitleDownloaded(subtitleFile: File) {
                            setSubtitles(subtitleFile)
                        }
                        override fun onFailedToDownload() {
                            Toast.makeText(applicationContext,
                                    "Failed to Get Selected Subtitle File",
                                    Toast.LENGTH_SHORT).show()
                        }
                    })
        }
    }

    private fun getDownloadDirectoryFor(torrent: com.hemendra.minitheater.data.Torrent): File {
        val path = """${Environment.getExternalStorageDirectory().absolutePath}/MiniTheater"""
        val downloadsDirectory = File(path)
        return File("""${downloadsDirectory.absolutePath}/${torrent.hash}""")
    }

    private fun getLoadedSubtitlesFile(): File? {
        movie?.let {
            val dir = getDownloadDirectoryFor(it.torrents[0])
            val newFile = File(dir, "subtitles.srt")
            if(newFile.exists() && newFile.length() > 0)
                return newFile
        }
        return null
    }

    private fun setSubtitles(file: File) {
        var subtitleFile = file

        movie?.let {
            val dir = getDownloadDirectoryFor(it.torrents[0])
            val newFile = File(dir, "subtitles.srt")
            if(Utils.moveFile(subtitleFile, newFile))
                subtitleFile = newFile
        }

        mediaPlayer?.let { mp ->
            subtitleView.setMediaPlayer(mp)
            subtitleView.setSubtitlesFile(subtitleFile)
            subtitleView.run()
            Toast.makeText(applicationContext, "Subtitles Loaded",
                    Toast.LENGTH_SHORT).show()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val path = Environment.getExternalStorageDirectory().absolutePath +
                    "/MiniTheater/subtitles"
            Utils.deleteDirectory(File(path))
            finish()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_IMMERSIVE)
    }

    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if(clickDetector?.onTouchEvent(event) == true) {
            return true
        }
        return false
    }

    private val downloadReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val mv = it.getSerializableExtra(DownloaderService.EXTRA_MOVIE) as Movie?
                mv?.let { m -> if(m.id == movie?.id) updateProgress(m) }
            }
        }
    }

    private fun updateProgress(m: Movie) {
        val downloadProgress = m.downloadProgress * 100f
        tvDownloadInfo.text = String.format(Locale.getDefault(),
                "Downloaded %.2f%%", downloadProgress)
        tryResettingMediaPlayer(downloadProgress)
    }

    private fun tryResettingMediaPlayer(downloadProgress: Float) {
        var max = Math.round((duration * (downloadProgress / 100f)) - 120000f)
        if (max < 0) max = 0

        if(rlProgress.visibility == View.VISIBLE
                && max > mediaPlayer?.currentPosition ?: 0) {
            if(activityShowing) {
                destroyMediaPlayerAndStopServer()
                Handler().postDelayed({ startMediaPlayerAndServer() }, 500)
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        startMediaPlayerAndServer()
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        destroyMediaPlayerAndStopServer()
    }

    private fun startMediaPlayerAndServer() {
        if(isDestroyed || isFinishing) return

        if(movie == null) {
            showMessage(this, "No Movie to Play!", Runnable { finish() })
            return
        }

        movie?.let {
            val file = DownloadsPresenter.getInstance().getTorrentFile(it.torrents[0])
            val twoMB = 10L * 1024L * 1024L
            if(file == null || file.length() < twoMB) {
                rlProgress.visibility = View.VISIBLE
                handler.postDelayed({ startMediaPlayerAndServer() }, 1000)
                return
            } else {
                rlProgress.visibility = View.GONE
            }

            localFileStreamingServer = LocalFileStreamingServer(file,
                    it.torrents[0].size_bytes)
            localFileStreamingServer?.init()
            localFileStreamingServer?.start()
        } ?: return

        mediaPlayer = MediaPlayer()
        // mediaPlayer?.setDataSource(this,
        //              Uri.parse("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4"))
        mediaPlayer?.setDataSource(localFileStreamingServer?.fileUrl)
        mediaPlayer?.setDisplay(surfaceView.holder)
        mediaPlayer?.setOnBufferingUpdateListener(this)
        mediaPlayer?.setOnCompletionListener(this)
        mediaPlayer?.setOnPreparedListener(this)
        mediaPlayer?.setScreenOnWhilePlaying(true)
        mediaPlayer?.setOnVideoSizeChangedListener(this)
        mediaPlayer?.setOnErrorListener(this)
        mediaPlayer?.setOnInfoListener(this)

        try {
            mediaPlayer?.prepare()
        }catch (e: IOException) {
            e.printStackTrace()
            mediaPlayer = null
            destroyMediaPlayerAndStopServer()
            handler.postDelayed({ startMediaPlayerAndServer() }, 1000)
            return
        }

        if(mediaController == null) {
            handler.post {
                mediaController = MediaController(this)
                mediaController?.setMediaPlayer(this)
                mediaController?.setAnchorView(llHolder)
                mediaController?.isEnabled = true
            }
        }
    }

    private fun destroyMediaPlayerAndStopServer() {
        positionBeforePaused = mediaPlayer?.currentPosition ?: 0
        subtitleView.stop()
        mediaPlayer?.stop()
        mediaPlayer?.reset()
        mediaPlayer?.release()
        mediaPlayer = null
        localFileStreamingServer?.stop()
        localFileStreamingServer = null
    }

    override fun onInfo(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        val sb = StringBuilder()
        when(what) {
            MediaPlayer.MEDIA_INFO_BUFFERING_END -> sb.append("MEDIA_INFO_BUFFERING_END")
            MediaPlayer.MEDIA_INFO_METADATA_UPDATE -> sb.append("MEDIA_INFO_METADATA_UPDATE")
            MediaPlayer.MEDIA_INFO_NOT_SEEKABLE -> sb.append("MEDIA_INFO_NOT_SEEKABLE")
            MediaPlayer.MEDIA_INFO_STARTED_AS_NEXT -> sb.append("MEDIA_INFO_STARTED_AS_NEXT")
            MediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT -> sb.append("MEDIA_INFO_SUBTITLE_TIMED_OUT")
            MediaPlayer.MEDIA_INFO_UNKNOWN -> sb.append("MEDIA_INFO_UNKNOWN")
            MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE -> sb.append("MEDIA_INFO_UNSUPPORTED_SUBTITLE")
            MediaPlayer.MEDIA_INFO_BUFFERING_START,
                    MediaPlayer.MEDIA_INFO_VIDEO_NOT_PLAYING,
                    MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING,
                    MediaPlayer.MEDIA_INFO_AUDIO_NOT_PLAYING,
                    703 /*MEDIA_INFO_NETWORK_BANDWIDTH*/ -> {
                sb.append("MEDIA_INFO_VIDEO_NOT_PLAYING " +
                        "| MEDIA_INFO_VIDEO_NOT_PLAYING " +
                        "| MEDIA_INFO_BAD_INTERLEAVING " +
                        "| MEDIA_INFO_NETWORK_BANDWIDTH : $what")
                if(DownloaderService.isDownloadingMovie(movie)) {
                    mediaPlayer?.stop()
                    rlProgress.visibility = View.VISIBLE
                } else {
                    tryResettingMediaPlayer(movie?.downloadProgress ?: 0f)
                }
            }
            MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                sb.append("MEDIA_INFO_VIDEO_RENDERING_START")
                rlProgress.visibility = View.GONE
            }
            MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING -> sb.append("MEDIA_INFO_VIDEO_TRACK_LAGGING")
            else -> sb.append(what)
        }
        Log.d(TAG, "onInfo: "+sb.toString())
        return false
    }

    override fun onBufferingUpdate(mp: MediaPlayer?, percent: Int) {

    }

    override fun onCompletion(mp: MediaPlayer?) {
    }

    override fun onPrepared(mp: MediaPlayer?) {
        isVideoReadyToBePlayed = true

        adjustSurfaceSize()
        mediaPlayer?.start()
        rlProgress.visibility = View.GONE
        mediaPlayerPrepared = true

        getLoadedSubtitlesFile()?.let { setSubtitles(it) }

        if(positionBeforePaused > 0)
            mediaPlayer?.seekTo(positionBeforePaused)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        if(mediaPlayerPrepared) adjustSurfaceSize()
    }

    private fun adjustSurfaceSize() {
        var targetWidth = resources.displayMetrics.widthPixels
        var targetHeight = resources.displayMetrics.heightPixels

        val videoWidth = mediaPlayer?.videoWidth ?: return
        val videoHeight = mediaPlayer?.videoHeight ?: return

        if(videoWidth > videoHeight) {
            targetHeight = ((targetWidth.toFloat() / videoWidth.toFloat()) * videoHeight.toFloat())
                    .toInt()
            targetWidth = LinearLayout.LayoutParams.MATCH_PARENT
        } else if(videoWidth < videoHeight) {
            targetWidth = ((targetHeight.toFloat() / videoHeight.toFloat()) * videoWidth.toFloat())
                    .toInt()
            targetHeight = LinearLayout.LayoutParams.MATCH_PARENT
        }

        val layoutParams = surfaceView.layoutParams
        layoutParams.width = targetWidth
        layoutParams.height = targetHeight

        surfaceView.layoutParams = layoutParams
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        val sb = StringBuilder()
        when(what) {
            MediaPlayer.MEDIA_ERROR_IO -> sb.append("MEDIA_ERROR_IO")
            MediaPlayer.MEDIA_ERROR_MALFORMED -> sb.append("MEDIA_ERROR_MALFORMED")
            MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK ->
                sb.append("MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK")
            MediaPlayer.MEDIA_ERROR_SERVER_DIED -> sb.append("MEDIA_ERROR_SERVER_DIED")
            MediaPlayer.MEDIA_ERROR_TIMED_OUT -> sb.append("MEDIA_ERROR_TIMED_OUT")
            MediaPlayer.MEDIA_ERROR_UNKNOWN -> sb.append("MEDIA_ERROR_UNKNOWN")
            MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> sb.append("MEDIA_ERROR_UNSUPPORTED")
            -38 -> {
                sb.append("what : $what | extra: $extra")
                tryResettingMediaPlayer(movie?.downloadProgress ?: 0f)
            }
            else -> sb.append("what : $what | extra: $extra")
        }
        Log.d(TAG, sb.toString())
        return false
    }

    override fun onVideoSizeChanged(mp: MediaPlayer?, width: Int, height: Int) {
    }

    override fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false

    override fun getDuration(): Int = mediaPlayer?.duration ?: 0

    override fun pause() {
        mediaPlayer?.pause()
    }

    override fun getBufferPercentage(): Int {
        return localFileStreamingServer?.bufferPercentage?.toInt() ?: 0
    }

    private fun maxSeekPosition(): Int {
        var max = Math.round((duration * (bufferPercentage / 100f)) - 120000f)
        if (max < 0) max = 0
        return max
    }

    @Synchronized override fun seekTo(pos: Int) {
        mediaPlayer?.let {
            synchronized(it) {
                it.seekTo(Math.min(pos, maxSeekPosition()))
            }
        }
    }

    override fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    override fun canSeekForward(): Boolean = true

    override fun canSeekBackward(): Boolean = true

    override fun start() {
        mediaPlayer?.start()
    }

    override fun getAudioSessionId(): Int  = 0

    override fun canPause(): Boolean  = true

}
