package com.hemendra.minitheater.view.home

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.view.*
import android.widget.Toast
import com.hemendra.minitheater.R
import kotlinx.android.synthetic.main.fragment_home.*

class HomeFragment: Fragment() {

    companion object {
        private var homeFragment: HomeFragment? = null

        fun getInstance(): HomeFragment {
            homeFragment?.let {
                return@let homeFragment
            }
            homeFragment = HomeFragment()
            return homeFragment as HomeFragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu?, menuInflater: MenuInflater?) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater!!.inflate(R.menu.main, menu)

        // Associate searchable configuration with the SearchView
        val searchManager = context!!.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView: SearchView = menu!!.findItem(R.id.action_search).actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity?.componentName))

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                Toast.makeText(activity, "Search For: $query", Toast.LENGTH_SHORT).show()
                searchView.clearFocus()
                return false
                // TODO("not implemented") // call the API to perform new search
            }

            override fun onQueryTextChange(query: String?): Boolean {
                Toast.makeText(activity, "Search For: $query", Toast.LENGTH_SHORT).show()
                return false
                // TODO("not implemented") // call the API to show trending
            }

        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
    }
}