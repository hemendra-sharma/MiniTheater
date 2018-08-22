package com.hemendra.minitheater.view

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.RelativeLayout
import com.dailymotion.android.player.sdk.PlayerWebView
import com.hemendra.minitheater.R
import com.hemendra.minitheater.secret.Credentials

class SearchActivity : AppCompatActivity() {

    var player: PlayerWebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        supportActionBar!!.hide()

        player = findViewById(R.id.player)
        player?.load("x6k12so", Credentials.getPlayerParameters())
        player?.setEventListener(playerEventListener)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        val params: RelativeLayout.LayoutParams = player?.layoutParams as RelativeLayout.LayoutParams
        params.width = RelativeLayout.LayoutParams.MATCH_PARENT
        params.height = RelativeLayout.LayoutParams.MATCH_PARENT
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            params.height = 215 * resources.displayMetrics.density.toInt()
        }
        player?.layoutParams = params
        super.onConfigurationChanged(newConfig)
    }

    private fun toggleFullScreenMode() {
        requestedOrientation = if(requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        else
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    private val playerEventListener: PlayerWebView.EventListener = PlayerWebView.EventListener {
        event: String, _: java.util.HashMap<String, String> ->
        when(event) {
            PlayerWebView.EVENT_FULLSCREEN_TOGGLE_REQUESTED -> toggleFullScreenMode()
        }
    }
}
