package com.hemendra.minitheater.view

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.WindowManager
import android.widget.RelativeLayout
import com.dailymotion.android.player.sdk.PlayerWebView
import com.hemendra.minitheater.R
import com.hemendra.minitheater.secret.Credentials
import kotlinx.android.synthetic.main.activity_search.*

class SearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        player.setEventListener(playerEventListener)
        player.setFullscreenButton(false)
        player.load("x26hv6c", Credentials.getPlayerParameters())
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        orientationChanged()
    }

    private fun orientationChanged() {
        val params: RelativeLayout.LayoutParams = player.layoutParams as RelativeLayout.LayoutParams
        params.width = RelativeLayout.LayoutParams.MATCH_PARENT
        params.height = RelativeLayout.LayoutParams.MATCH_PARENT
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            params.height = 215 * resources.displayMetrics.density.toInt()
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            player.setFullscreenButton(false)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            player.setFullscreenButton(true)
        }
        player.layoutParams = params
    }

    private fun toggleFullScreenMode() {
        requestedOrientation = if(resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
        orientationChanged()
    }

    private val playerEventListener: PlayerWebView.EventListener = PlayerWebView.EventListener {
        event: String, _: java.util.HashMap<String, String> ->
        when(event) {
            PlayerWebView.EVENT_FULLSCREEN_TOGGLE_REQUESTED -> toggleFullScreenMode()
        }
    }
}
