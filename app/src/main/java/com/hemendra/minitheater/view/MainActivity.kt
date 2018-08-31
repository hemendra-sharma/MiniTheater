package com.hemendra.minitheater.view

import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.widget.Toast
import com.hemendra.minitheater.R
import com.hemendra.minitheater.view.explorer.ExplorerFragment
import com.hemendra.minitheater.view.player.PlayerFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
                Toast.makeText(this, R.string.explore, Toast.LENGTH_SHORT).show()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_downloads-> {
                Toast.makeText(this, R.string.downloads, Toast.LENGTH_SHORT).show()
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
            if(ExplorerFragment.instance.onBackPressed()) return
        }
        finish()
    }

    private fun showExplorerFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.place_holder, ExplorerFragment.instance)
        transaction.commitAllowingStateLoss()
    }

    private fun showPlayerFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.place_holder, PlayerFragment.getInstance())
        transaction.addToBackStack("video")
        transaction.commitAllowingStateLoss()
    }
}
