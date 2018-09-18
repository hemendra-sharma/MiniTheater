package com.hemendra.minitheater.view

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.View
import com.hemendra.minitheater.R
import com.hemendra.minitheater.data.Movie
import com.hemendra.minitheater.presenter.ImagesPresenter
import com.hemendra.minitheater.utils.RemoteConfig
import com.hemendra.minitheater.view.downloader.DownloaderFragment
import com.hemendra.minitheater.view.explorer.ExplorerFragment
import com.hemendra.minitheater.view.listeners.OnMovieDownloadClickListener
import com.hemendra.minitheater.view.listeners.OnMovieItemClickListener
import com.hemendra.minitheater.view.explorer.DetailsFragment
import com.hemendra.minitheater.view.more.FindMoreFragment
import com.hemendra.minitheater.view.player.PlayerActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val explorerFragment = ExplorerFragment()
    private val detailsFragment: DetailsFragment = DetailsFragment()
    private val downloaderFragment: DownloaderFragment = DownloaderFragment()
    private val findMoreFragment: FindMoreFragment = FindMoreFragment()

    private var runtimePermissionManager: RuntimePermissionManager? = null

    companion object {
        private const val EXPLORER_FRAGMENT_TAG = "explorer"
        private const val DETAILS_FRAGMENT_TAG = "details"
        private const val FIND_MORE_FRAGMENT_TAG = "find_more"
        private const val DOWNLOADS_FRAGMENT_TAG = "downloads"
    }

    private var movieToAddToDownloads: Movie? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        explorerFragment.onMovieItemClickListener = onMovieItemClickListener

        runtimePermissionManager = RuntimePermissionManager(this)
        if(runtimePermissionManager?.askForPermissions() == true) beginConfig()
    }

    override fun onNewIntent(i: Intent?) {
        super.onNewIntent(i)
        if(intent.getBooleanExtra("from_downloading_notification", false))
            navigation.selectedItemId = R.id.navigation_downloads
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        runtimePermissionManager?.onRequestPermissionsResult(requestCode, permissions, grantResults,
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
                if(currentFragmentTag() != EXPLORER_FRAGMENT_TAG) {
                    showExplorerFragment()
                }
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_search -> {
                if(currentFragmentTag() != FIND_MORE_FRAGMENT_TAG) {
                    showFindMoreFragment()
                }
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_downloads -> {
                if(currentFragmentTag() != DOWNLOADS_FRAGMENT_TAG) {
                    showDownloaderFragment()
                }
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

    private fun resetNavigationSelection() {
        val current = currentFragmentTag()
        if(current == EXPLORER_FRAGMENT_TAG || current == DETAILS_FRAGMENT_TAG) {
            selectNavigationItem(R.id.navigation_explore)
        } else if(current == DOWNLOADS_FRAGMENT_TAG) {
            selectNavigationItem(R.id.navigation_downloads)
        } else if(current == FIND_MORE_FRAGMENT_TAG) {
            selectNavigationItem(R.id.navigation_search)
        }
    }

    private fun selectNavigationItem(actionId: Int) {
        val size = navigation.menu.size()
        for(i in 0 until size) {
            val item = navigation.menu.getItem(i)
            if(actionId == item.itemId) item.isChecked = true
        }
    }

    private val onMovieItemClickListener = object: OnMovieItemClickListener {
        override fun onMovieItemClicked(movie: Movie) {
            showDetailsFragment(movie)
        }
    }

    private val onMovieDownloadClickListener = object: OnMovieDownloadClickListener {

        override fun onDownloadClicked(movie: Movie) {
            movieToAddToDownloads = movie
            navigation.selectedItemId = R.id.navigation_downloads
        }

        override fun onPlayClicked(movie: Movie) {
            navigation.selectedItemId = R.id.navigation_downloads
            showPlayerActivity(movie)
        }

    }

    override fun onBackPressed() {
        when(currentFragmentTag()) {
            EXPLORER_FRAGMENT_TAG -> {
                if(explorerFragment.onBackPressed()) return
                finish()
                return
            }
            FIND_MORE_FRAGMENT_TAG -> if(findMoreFragment.onBackPressed()) return
        }

        supportFragmentManager.popBackStackImmediate()
        resetNavigationSelection()

        if(supportFragmentManager.backStackEntryCount == 0)
            finish()
    }

    private fun showExplorerFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.place_holder, explorerFragment, EXPLORER_FRAGMENT_TAG)
        transaction.addToBackStack(EXPLORER_FRAGMENT_TAG)
        transaction.commitAllowingStateLoss()
    }

    private fun showDetailsFragment(movie: Movie) {
        detailsFragment.setMovie(movie)
        val transaction = supportFragmentManager.beginTransaction()
        detailsFragment.setMovieDownloadClickListener(onMovieDownloadClickListener)
        transaction.replace(R.id.place_holder, detailsFragment, DETAILS_FRAGMENT_TAG)
        transaction.addToBackStack(DETAILS_FRAGMENT_TAG)
        transaction.commitAllowingStateLoss()
    }

    private fun showFindMoreFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        findMoreFragment.setMovieDownloadClickListener(onMovieDownloadClickListener)
        transaction.replace(R.id.place_holder, findMoreFragment, FIND_MORE_FRAGMENT_TAG)
        transaction.addToBackStack(FIND_MORE_FRAGMENT_TAG)
        transaction.commitAllowingStateLoss()
    }

    private fun showDownloaderFragment() {
        movieToAddToDownloads?.let {
            downloaderFragment.setMovieToAdd(it)
            movieToAddToDownloads = null
        }
        val transaction = supportFragmentManager.beginTransaction()
        downloaderFragment.setMovieClickListener(onMovieDownloadClickListener)
        transaction.replace(R.id.place_holder, downloaderFragment, DOWNLOADS_FRAGMENT_TAG)
        transaction.addToBackStack(DOWNLOADS_FRAGMENT_TAG)
        transaction.commitAllowingStateLoss()
    }

    private fun showPlayerActivity(movie: Movie) {
        val intent = Intent(applicationContext, PlayerActivity::class.java)
        intent.putExtra("movie", movie)
        startActivity(intent)
    }

    override fun onDestroy() {
        ImagesPresenter.getInstance(this).close()
        super.onDestroy()
    }

}
