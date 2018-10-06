package com.hemendra.minitheater.view.player

import android.annotation.SuppressLint
import android.content.*
import android.content.res.Configuration
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.*
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.data.MovieObjectType
import com.hemendra.minitheater.data.Subtitle
import com.hemendra.minitheater.presenter.DownloadsPresenter
import com.hemendra.minitheater.presenter.SubtitlesPresenter
import com.hemendra.minitheater.presenter.listeners.SubtitleDownloadListener
import com.hemendra.minitheater.presenter.listeners.SubtitlesListDownloadListener
import com.hemendra.minitheater.service.DownloaderService
import com.hemendra.minitheater.utils.Utils
import com.hemendra.minitheater.view.showMessage
import kotlinx.android.synthetic.main.activity_player.*
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class PlayerActivity : AppCompatActivity(), SurfaceHolder.Callback,
        MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnVideoSizeChangedListener,
        MediaPlayer.OnErrorListener, MediaController.MediaPlayerControl,
        MediaPlayer.OnInfoListener, View.OnTouchListener {

    companion object {
        private const val TAG = "PlayerActivity"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var mediaController: MediaController? = null
    private var isVideoReadyToBePlayed = false
    private var movie: Movie? = null
    private var movieFile: File? = null
    private val handler = Handler()
    private var mediaPlayerPrepared = false
    private var clickDetector: GestureDetector? = null
    private var positionBeforePaused = 0
    private var activityShowing = false
    private var pausedByUser = false
    private var subtitlesAdapter: SubtitlesListAdapter? = null
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var controlsLocked = false
    private val audioTracks = HashMap<MediaPlayer.TrackInfo, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        volumeControlStream = AudioManager.STREAM_MUSIC

        movie = intent.getSerializableExtra("movie") as Movie

        surfaceView.holder.addCallback(this)
        llHolder.setOnTouchListener(this)

        setupSubtitlesView()
        setupGestures()
        setupLock()

        showSystemUI()

        showHints()
    }

    private fun showHints() {
        val prefs = getSharedPreferences("PlayerHint", Context.MODE_PRIVATE)
        if(prefs?.getBoolean("hint_shown", false) == false) {
            rlHints.visibility = View.VISIBLE
            tvGotIt.setOnClickListener { rlHints.visibility = View.GONE }
            prefs.edit().putBoolean("hint_shown", true).apply()
        }
    }

    override fun onResume() {
        super.onResume()
        activityShowing = true
        val mgr = LocalBroadcastManager.getInstance(applicationContext)
        val filter = IntentFilter(DownloaderService.ACTION_PROGRESS_UPDATE)
        mgr.registerReceiver(downloadReceiver, filter)
        if(!pausedByUser) mediaPlayer?.start()
    }

    override fun onPause() {
        super.onPause()
        activityShowing = false
        mediaPlayer?.pause()
        val mgr = LocalBroadcastManager.getInstance(applicationContext)
        mgr.unregisterReceiver(downloadReceiver)
        movie?.watchingProgress = mediaPlayer?.currentPosition ?: 0
        movie?.let {
            it.watchingProgress = mediaPlayer?.currentPosition ?: 0
            DownloadsPresenter.getInstance().updateMovie(it)
        }
    }

    private var firstBackPressedAt = 0L

    override fun onBackPressed() {
        if(System.currentTimeMillis() - firstBackPressedAt < 2000)
            super.onBackPressed()
        else {
            firstBackPressedAt = System.currentTimeMillis()
            Toast.makeText(this, "Press Back Again to Exit !", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSubtitlesView() {
        if(movie?.movieObjectType == MovieObjectType.EXTRA) {
            ivSubtitles.visibility = View.GONE
            llSubtitlesDelay.visibility = View.GONE
        } else {
            ivSubtitles.visibility = View.VISIBLE
            llSubtitlesDelay.visibility = View.VISIBLE
            ivSubtitles.setOnClickListener { _ ->
                if (rlSubtitles.visibility == View.VISIBLE) {
                    rlSubtitles.visibility = View.GONE
                } else {
                    rlSubtitles.visibility = View.VISIBLE
                    mediaController?.hide()
                    tvDownloadInfo.visibility = View.GONE
                    ivLock.visibility = View.GONE
                    ivAudioTracks.visibility = View.GONE
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

            ivSubtitlesMinus.setOnClickListener { _ ->
                subtitleView.decreaseDelayMS()
                val delay = subtitleView.getDelayMS()
                if(delay > 0) tvSubtitlesDelay.text =
                        String.format(Locale.getDefault(), "+ %s ms", delay)
                else tvSubtitlesDelay.text =
                        String.format(Locale.getDefault(), "%s ms", delay)
            }

            ivSubtitlesPlus.setOnClickListener { _ ->
                subtitleView.increaseDelayMS()
                val delay = subtitleView.getDelayMS()
                if(delay > 0) tvSubtitlesDelay.text =
                        String.format(Locale.getDefault(), "+ %s ms", delay)
                else tvSubtitlesDelay.text =
                        String.format(Locale.getDefault(), "%s ms", delay)
            }
        }

        lvSubtitles.onItemClickListener = subtitlesListItemClickListener
    }

    private val subtitlesListItemClickListener = AdapterView.OnItemClickListener {
        _, _, position, _ ->
        subtitlesAdapter?.let {
            rlSubtitles.visibility = View.GONE
            ivSubtitles.visibility = View.GONE
            llSubtitlesDelay.visibility = View.GONE
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

    private fun setupLock() {
        ivLock.setOnClickListener { _ ->
            controlsLocked = !controlsLocked
            if(controlsLocked) {
                ivLock.setImageResource(R.drawable.ic_lock_outline_black_24dp)
                hideSystemUI()
                mediaController?.hide()
                ivSubtitles.visibility = View.GONE
                llSubtitlesDelay.visibility = View.GONE
                tvDownloadInfo.visibility = View.GONE
                ivLock.visibility = View.GONE
                ivAudioTracks.visibility = View.GONE
                rlSubtitles.visibility = View.GONE
                svAudioTracks.visibility = View.GONE
            } else {
                ivLock.setImageResource(R.drawable.ic_lock_open_black_24dp)
                showSystemUI()
                mediaController?.show(0)
                if (movie?.movieObjectType != MovieObjectType.EXTRA) {
                    ivSubtitles.visibility = View.VISIBLE
                    llSubtitlesDelay.visibility = View.VISIBLE
                }
                tvDownloadInfo.visibility = View.VISIBLE
                ivLock.visibility = View.VISIBLE
                if(audioTracks.size > 1)
                    ivAudioTracks.visibility = View.VISIBLE
            }
        }
    }

    private var initialSeekPosition = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if(event?.action == MotionEvent.ACTION_UP) {
            initialVolumeLevel = 0
            maxVolumeLevel = 0
            initialBrightnessLevel = 0
            initialSeekPosition = 0
            tvInfo.visibility = View.GONE
        }
        if(clickDetector?.onTouchEvent(event) == true) {
            return true
        }
        return false
    }

    private fun setupGestures() {
        clickDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if(controlsLocked) {
                    if(ivLock.visibility == View.VISIBLE)
                        ivLock.visibility = View.GONE
                    else
                        ivLock.visibility = View.VISIBLE
                } else {
                    if (mediaController?.isShowing == true) {
                        hideSystemUI()
                        mediaController?.hide()
                        ivSubtitles.visibility = View.GONE
                        llSubtitlesDelay.visibility = View.GONE
                        tvDownloadInfo.visibility = View.GONE
                        ivLock.visibility = View.GONE
                        ivAudioTracks.visibility = View.GONE
                    } else {
                        showSystemUI()
                        mediaController?.show(0)
                        if (movie?.movieObjectType != MovieObjectType.EXTRA) {
                            ivSubtitles.visibility = View.VISIBLE
                            llSubtitlesDelay.visibility = View.VISIBLE
                        }
                        tvDownloadInfo.visibility = View.VISIBLE
                        ivLock.visibility = View.VISIBLE
                        if(audioTracks.size > 1)
                            ivAudioTracks.visibility = View.VISIBLE
                    }
                    rlSubtitles.visibility = View.GONE
                    svAudioTracks.visibility = View.GONE
                }
                return true
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent?,
                                  distanceX: Float, distanceY: Float): Boolean {
                if(controlsLocked)
                    return false

                e1?.let { event1 ->
                    e2?.let { event2 ->
                        if(event1.rawY < screenHeight.toFloat() * 0.9f
                            && event2.rawY < screenHeight.toFloat() * 0.9f) {
                            // volume and brightness controls
                            val diffY = event1.rawY - event2.rawY
                            val singleStep = screenHeight.toFloat() / 100f
                            val percentChange = diffY / singleStep

                            if (event1.rawX > screenWidth / 2
                                    && event2.rawX > screenWidth / 2) {
                                // volume controls
                                if (initialVolumeLevel == 0
                                        || maxVolumeLevel == 0) {
                                    saveInitialVolumeLevel()
                                }
                                changeVolumePercent(percentChange)
                            } else if (event1.rawX < screenWidth / 2
                                    && event2.rawX < screenWidth / 2) {
                                // screen brightness
                                if (initialBrightnessLevel == 0
                                        || maxBrightnessLevel == 0) {
                                    saveInitialBrightnessLevel()
                                }
                                changeBrightnessPercent(percentChange)
                            }
                            return true
                        } else if(event1.rawY > screenHeight.toFloat() * 0.9f
                                && event2.rawY > screenHeight.toFloat() * 0.9f) {
                            mediaPlayer?.let {
                                // seek controls
                                val multiplier = 10f // seek slower. take bigger steps.
                                val diffX = event2.rawX - event1.rawX
                                val singleStep = (screenWidth.toFloat() / 100f) * multiplier
                                val percent = diffX / singleStep
                                if(initialSeekPosition == 0)
                                    initialSeekPosition = it.currentPosition
                                else {
                                    val change = ((Math.abs(percent) / 100f) *
                                            it.duration.toFloat()).toInt()
                                    val newPosition = if(percent < 0) initialSeekPosition - change
                                    else initialSeekPosition + change
                                    seekTo(newPosition)
                                    if(percent > 0)
                                        tvInfo.text = String.format(Locale.getDefault(),
                                                "+ %d seconds", change / 1000)
                                    else
                                        tvInfo.text = String.format(Locale.getDefault(),
                                                "- %d seconds", change / 1000)
                                    tvInfo.visibility = View.VISIBLE
                                }
                                return true
                            }
                        }
                    }
                }
                return false
            }

            private var pausedByLongPress = false

            override fun onLongPress(e: MotionEvent?) {
                if(mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                    pausedByLongPress = true
                }
            }

            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                return if(pausedByLongPress) {
                    pausedByLongPress = false
                    mediaPlayer?.start()
                    true
                } else false
            }
        })
    }

    private var initialVolumeLevel = 0
    private var maxVolumeLevel = 0
    private fun saveInitialVolumeLevel() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        audioManager?.let {
            initialVolumeLevel = it.getStreamVolume(AudioManager.STREAM_MUSIC)
            maxVolumeLevel = it.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        }
    }

    private fun changeVolumePercent(percent: Float) {
        val changeLevel = ((Math.abs(percent) / 100f) * maxVolumeLevel.toFloat()).toInt()
        var newLevel = if(percent < 0) initialVolumeLevel - changeLevel
        else initialVolumeLevel + changeLevel
        if(newLevel < 0) newLevel = 0
        else if(newLevel > maxVolumeLevel) newLevel = maxVolumeLevel
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, newLevel, 0)
        val newPercent = ((newLevel.toFloat() / maxVolumeLevel.toFloat()) * 100f).toInt()
        tvInfo.text = String.format(Locale.getDefault(), "Volume %d%%", newPercent)
        tvInfo.visibility = View.VISIBLE
    }

    private var initialBrightnessLevel = 0
    private val maxBrightnessLevel = 255
    private var brightnessMessageShowing = false
    private fun saveInitialBrightnessLevel() {
        initialBrightnessLevel = Settings.System.getInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS)
    }

    private fun changeBrightnessPercent(percent: Float) {
        if(Settings.System.canWrite(applicationContext)) {
            val changeLevel = ((Math.abs(percent) / 100f) * maxBrightnessLevel.toFloat()).toInt()
            var newLevel = if(percent < 0) initialBrightnessLevel - changeLevel
            else initialBrightnessLevel + changeLevel
            if(newLevel <= 0) newLevel = 1
            else if(newLevel > maxBrightnessLevel) newLevel = maxBrightnessLevel

            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, newLevel)

            val newPercent = ((newLevel.toFloat() / maxBrightnessLevel.toFloat()) * 100f).toInt()
            tvInfo.text = String.format(Locale.getDefault(), "Brightness %d%%", newPercent)
            tvInfo.visibility = View.VISIBLE
        } else if(!brightnessMessageShowing) {
            brightnessMessageShowing = true
            showMessage(this, "The app requires permission to change screen brightness.",
                    Runnable {
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        intent.data = Uri.parse("package:$packageName")
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivityForResult(intent, 1001)
                        brightnessMessageShowing = false
                    })
        }
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
        screenWidth = width
        screenHeight = height
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
            if(it.streamingURL.isNotEmpty()) {
                mediaPlayer = MediaPlayer()
                try {
                    mediaPlayer?.setDataSource(it.streamingURL)
                } catch (e: IOException) {
                    e.printStackTrace()
                    mediaPlayer = null
                    destroyMediaPlayerAndStopServer()
                    onBackPressed()
                    Toast.makeText(this, "An Error Occurred !", Toast.LENGTH_SHORT).show()
                    return
                }
            } else {
                movieFile = DownloadsPresenter.getInstance().getTorrentFile(it.torrents[0])
                if (movieFile == null) {
                    Log.d(TAG, "file not created yet")
                    rlProgress.visibility = View.VISIBLE
                    handler.postDelayed({ startMediaPlayerAndServer() }, 1000)
                    return
                }

                movieFile?.let { file ->
                    val tenMB = 10L * 1024L * 1024L
                    if (file.length() < tenMB) {
                        Log.d(TAG, "file length is small: ${file.length() / 1024f / 1024f} MB")
                        rlProgress.visibility = View.VISIBLE
                        handler.postDelayed({ startMediaPlayerAndServer() }, 1000)
                        return
                    } else {
                        rlProgress.visibility = View.GONE
                    }

                    mediaPlayer = MediaPlayer()
                    try {
                        mediaPlayer?.setDataSource(file.absolutePath)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        mediaPlayer = null
                        destroyMediaPlayerAndStopServer()
                        handler.postDelayed({ startMediaPlayerAndServer() }, 1000)
                        return
                    }
                } ?: return
            }
        } ?: return
        mediaPlayer?.setDisplay(surfaceView.holder)
        mediaPlayer?.setVolume(1f, 1f)
        mediaPlayer?.setOnBufferingUpdateListener(this)
        mediaPlayer?.setOnCompletionListener(this)
        mediaPlayer?.setOnPreparedListener(this)
        mediaPlayer?.setScreenOnWhilePlaying(true)
        mediaPlayer?.setOnVideoSizeChangedListener(this)
        mediaPlayer?.setOnErrorListener(this)
        mediaPlayer?.setOnInfoListener(this)

        try {
            mediaPlayer?.prepareAsync()
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

    override fun onBufferingUpdate(mp: MediaPlayer?, percent: Int) {}

    override fun onCompletion(mp: MediaPlayer?) {}

    override fun onPrepared(mp: MediaPlayer?) {
        isVideoReadyToBePlayed = true

        audioTracks.clear()
        mediaPlayer?.trackInfo?.let {
            for(i in 0 until it.size) {
                if(it[i].trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO)
                    audioTracks[it[i]] = i
            }
        }
        if(audioTracks.size > 1) {
            if(mediaController?.isShowing == true)
                ivAudioTracks.visibility = View.VISIBLE
            mediaPlayer?.selectTrack(audioTracks.values.elementAt(0))
            fillAudioTrackViews()
            ivAudioTracks.setOnClickListener {
                if(svAudioTracks.visibility == View.VISIBLE)
                    svAudioTracks.visibility = View.GONE
                else
                    svAudioTracks.visibility = View.VISIBLE
            }
        }

        adjustSurfaceSize()
        mediaPlayer?.start()
        if(movie?.watchingProgress ?: 0 > 0) {
            seekTo(movie?.watchingProgress ?: 0)
        }
        pausedByUser = false
        rlProgress.visibility = View.GONE
        mediaPlayerPrepared = true

        getLoadedSubtitlesFile()?.let { setSubtitles(it) }

        if(positionBeforePaused > 0)
            mediaPlayer?.seekTo(positionBeforePaused)
    }

    private fun fillAudioTrackViews() {
        llAudioTracks.removeAllViews()
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        for(trackInfo in audioTracks.keys) {
            val tv = TextView(this)
            tv.layoutParams = params
            tv.text = trackInfo.language
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            tv.setTextColor(Color.WHITE)
            tv.gravity = Gravity.CENTER_VERTICAL or Gravity.START
            tv.setPadding(5, 10, 5, 10)
            tv.tag = audioTracks[trackInfo]
            tv.setOnClickListener(onAudioTrackClicked)
            llAudioTracks.addView(tv)
        }
    }

    private val onAudioTrackClicked = View.OnClickListener { v ->
        mediaPlayer?.selectTrack(v.tag as Int)
        svAudioTracks.visibility = View.GONE
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
                return true
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
        pausedByUser = true
    }

    override fun getBufferPercentage(): Int {
        return if(movie?.streamingURL?.isNotEmpty() == true) {
            100
        } else {
            val downloadedSize = (movieFile?.length() ?: 0).toDouble()
            val expectedSize = (movie?.torrents?.get(0)?.size_bytes ?: 0).toDouble()
            ((downloadedSize / expectedSize) * 100f).toInt()
        }
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
        pausedByUser = false
    }

    override fun getAudioSessionId(): Int  = 0

    override fun canPause(): Boolean  = true

}