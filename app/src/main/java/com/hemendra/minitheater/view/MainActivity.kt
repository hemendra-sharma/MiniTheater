package com.hemendra.minitheater.view

import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.View
import android.widget.Toast
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        explorerFragment.onMovieItemClickListener = onMovieItemClickListener

        if(runtimePermissionManager.askForPermissions()) beginConfig()
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
                while(supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStackImmediate()
                }
                for(fragment in supportFragmentManager.fragments) {
                    if(fragment !is ExplorerFragment) {
                        supportFragmentManager
                                .beginTransaction()
                                .remove(fragment)
                                .commitAllowingStateLoss()
                    }
                }
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_downloads-> {
                showDownloaderFragment()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                Toast.makeText(this, R.string.notifications, Toast.LENGTH_SHORT).show()
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onBackPressed() {
        if(supportFragmentManager.backStackEntryCount == 0) {
            if(explorerFragment.onBackPressed()) return
        } else if(supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
            val entry = supportFragmentManager
                    .getBackStackEntryAt(supportFragmentManager.backStackEntryCount-1)
            if(entry.name == DETAILS_FRAGMENT_TAG) {
                navigation.selectedItemId = R.id.navigation_explore
            } else if(entry.name == DOWNLOADS_FRAGMENT_TAG) {
                navigation.selectedItemId = R.id.navigation_downloads
            }
            return
        }
        finish()
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
        transaction.add(R.id.place_holder, downloaderFragment)
        transaction.addToBackStack(DOWNLOADS_FRAGMENT_TAG)
        transaction.commitAllowingStateLoss()
    }

}
