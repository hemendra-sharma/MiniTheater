package com.hemendra.minitheater.view

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.View
import android.widget.Toast
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.presenter.DownloadsPresenter
import com.hemendra.minitheater.utils.RemoteConfig
import com.hemendra.minitheater.view.downloader.DownloaderFragment
import com.hemendra.minitheater.view.explorer.ExplorerFragment
import com.hemendra.minitheater.view.listeners.OnMovieDownloadClickListener
import com.hemendra.minitheater.view.listeners.OnMovieItemClickListener
import com.hemendra.minitheater.view.explorer.DetailsFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val explorerFragment = ExplorerFragment()
    private val runtimePermissionManager = RuntimePermissionManager(this)

    private val DETAILS_FRAGMENT_TAG = "details"
    private val DOWNLOADS_FRAGMENT_TAG = "downloads"

    private var movieToAddToDownloads: Movie? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        explorerFragment.onMovieItemClickListener = onMovieItemClickListener

        if(runtimePermissionManager.askForPermissions()) beginConfig()
    }

    override fun onNewIntent(i: Intent?) {
        super.onNewIntent(i)
        if(intent.getBooleanExtra("from_downloading_notification", false))
            navigation.selectedItemId = R.id.navigation_downloads
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        runtimePermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults,
                Runnable { beginConfig() })
    }

    private fun beginConfig() {
        RemoteConfig.getInstance().initialize(
                Runnable { configComplete() },
                Runnable {
                    showMessage(this, """Failed to initialize!
                        |Please check your internet connection and try again.""".trimMargin(),
                            Runnable { finishAffinity() })
                })
    }

    private fun configComplete() {
        rlLogo.visibility = View.GONE
        navigation.setOnNavigationItemSelectedListener(navigationListener)
        showExplorerFragment()
        if(intent.getBooleanExtra("from_downloading_notification", false))
            navigation.selectedItemId = R.id.navigation_downloads
    }

    /**
     * Take the action bar from fragment and attach it to this activity
     */
    override fun setSupportActionBar(toolbar: Toolbar?) {
        super.setSupportActionBar(toolbar)
        invalidateOptionsMenu()
    }

    /**
     * refresh the action bar
     * @param menu The menu to inflate
     * @return Return FALSE because we want to handle search-view implementation on
     * fragment itself.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        setupActionBar()
        return false
    }

    /**
     * Add logo to action bar
     */
    private fun setupActionBar() {
        supportActionBar?.setLogo(R.drawable.main_activity_logo)
        supportActionBar?.setDisplayUseLogoEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private val navigationListener = BottomNavigationView.OnNavigationItemSelectedListener {
        item -> when (item.itemId) {
            R.id.navigation_explore -> {
                var tag = currentFragmentTag()
                if(tag == DETAILS_FRAGMENT_TAG) {
                    while (supportFragmentManager.backStackEntryCount > 0) {
                        supportFragmentManager.popBackStackImmediate()
                    }
                } else {
                    while (supportFragmentManager.backStackEntryCount > 0
                            && tag != DETAILS_FRAGMENT_TAG) {
                        supportFragmentManager.popBackStackImmediate()
                        tag = currentFragmentTag()
                    }
                }
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_downloads-> {
                if(getSelectedNavigationItem() != R.id.navigation_downloads)
                    showDownloaderFragment()
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    private fun currentFragmentTag(): String {
        if(supportFragmentManager.backStackEntryCount == 0) return ""

        val entry = supportFragmentManager
                .getBackStackEntryAt(supportFragmentManager.backStackEntryCount-1)
        return entry.name ?: ""
    }

    override fun onBackPressed() {
        if(supportFragmentManager.backStackEntryCount == 0) {
            if(explorerFragment.onBackPressed()) return
        } else if(supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
            resetNavigationSelection()
            return
        }
        finish()
    }

    private fun resetNavigationSelection() {
        val current = currentFragmentTag()
        if(current == DETAILS_FRAGMENT_TAG
                || supportFragmentManager.backStackEntryCount == 0) {
            selectNavigationItem(R.id.navigation_explore)
        } else if(current == DOWNLOADS_FRAGMENT_TAG) {
            selectNavigationItem(R.id.navigation_downloads)
        }
    }

    private fun selectNavigationItem(actionId: Int) {
        val size = navigation.menu.size()
        for(i in 0 until size) {
            val item = navigation.menu.getItem(i)
            if(actionId == item.itemId) item.isChecked = true
        }
    }

    private fun getSelectedNavigationItem(): Int {
        val size = navigation.menu.size()
        for(i in 0 until size) {
            val item = navigation.menu.getItem(i)
            if(item.isChecked) return item.itemId
        }
        return 0
    }

    private val onMovieItemClickListener = object: OnMovieItemClickListener {
        override fun onMovieItemClicked(movie: Movie) {
            showDetailsFragment(movie)
        }
    }

    private fun showExplorerFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.place_holder, explorerFragment)
        transaction.commitAllowingStateLoss()
    }

    private val onMovieDownloadClickListener = object: OnMovieDownloadClickListener {

        override fun onDownloadClicked(movie: Movie) {
            movieToAddToDownloads = movie
            navigation.selectedItemId = R.id.navigation_downloads
        }

        override fun onPlayClicked(movie: Movie) {

        }

    }

    private fun showDetailsFragment(movie: Movie) {
        val transaction = supportFragmentManager.beginTransaction()
        val detailsFragment = DetailsFragment()
        detailsFragment.setMovie(movie)
        detailsFragment.setMovieDownloadClickListener(onMovieDownloadClickListener)
        transaction.add(R.id.place_holder, detailsFragment)
        transaction.addToBackStack(DETAILS_FRAGMENT_TAG)
        transaction.commitAllowingStateLoss()
    }

    private fun showDownloaderFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        val downloaderFragment = DownloaderFragment()
        movieToAddToDownloads?.let {
            downloaderFragment.setMovieToAdd(it)
            movieToAddToDownloads = null
        }
        transaction.add(R.id.place_holder, downloaderFragment)
        transaction.addToBackStack(DOWNLOADS_FRAGMENT_TAG)
        transaction.commitAllowingStateLoss()
    }

}
