package com.hemendra.minitheater.view

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

import com.hemendra.minitheater.R
import com.hemendra.minitheater.remote.RemoteConfig

class SplashScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        beginConfig()
    }

    private fun beginConfig() {
        RemoteConfig.getInstance().initialize(
                Runnable { startMainActivity() },
                Runnable {
                    showMessage(this, """Failed to initialize!
                        |Please check your internet connection and try again.""".trimMargin(),
                            Runnable { finishAffinity() })
                })
    }

    private fun startMainActivity() {
        val intent = Intent(applicationContext, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        return
    }
}
