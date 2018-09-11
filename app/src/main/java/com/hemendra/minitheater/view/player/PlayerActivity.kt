package com.hemendra.minitheater.view.player

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.WindowManager
import android.widget.MediaController
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.presenter.DownloadsPresenter
import com.hemendra.minitheater.utils.LocalFileStreamingServer
import com.hemendra.minitheater.utils.StreamListener
import com.hemendra.minitheater.view.showMessage
import kotlinx.android.synthetic.main.activity_player.*

class PlayerActivity : AppCompatActivity(), StreamListener {

    //private var stream: StreamOverHttp? = null
    private var localFileStreamingServer: LocalFileStreamingServer? = null
    private var controller: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)

        controller = MediaController(this)

        val movie: Movie = intent.getSerializableExtra("movie") as Movie

        videoView.setMediaController(controller)
        controller?.setAnchorView(videoView)

        videoView.setOnErrorListener { mp, what, extra ->
            Log.e("PlayerActivity", "error: $what")
            false
        }

        videoView.setOnInfoListener { mp, what, extra ->
            Log.i("PlayerActivity", "info: $what")
            false
        }

        videoView.setOnPreparedListener {
            videoView.start()
        }

        val file = DownloadsPresenter.getInstance().getTorrentFile(movie.torrents[0])
        if(file != null) {
            localFileStreamingServer = LocalFileStreamingServer(file, movie.torrents[0].size_bytes)
            localFileStreamingServer?.init()
            localFileStreamingServer?.start()
            videoView.setVideoURI(Uri.parse(localFileStreamingServer?.fileUrl))

            /*stream = StreamOverHttp(file, "video/mp4", movie.torrents[0].size_bytes, this)
            if(stream != null) {
                val uri = stream?.getUri(file.name)
                if (uri != null) {
                    videoView.setVideoURI(uri)
                } else {
                    showMessage(this, "Failed to Begin Readable Stream", Runnable { finish() })
                }
            } else {
                showMessage(this, "Failed to Initialize Stream", Runnable { finish() })
            }*/
        } else {
            showMessage(this, "Failed to Read File", Runnable { finish() })
        }
    }

    override fun onPause() {
        super.onPause()
        //player.pause()
        videoView.pause()
    }

    override fun onDestroy() {
        //stream?.close()
        localFileStreamingServer?.stop()
        super.onDestroy()
    }

    override fun onBufferingStarted() {
        videoView.pause()
    }

    override fun onBufferingStopped() {
        videoView.start()
    }
}
