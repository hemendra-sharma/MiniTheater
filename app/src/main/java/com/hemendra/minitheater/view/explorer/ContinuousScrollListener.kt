package com.hemendra.minitheater.view.explorer

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView

abstract class ContinuousScrollListener(private var mLayoutManager: LinearLayoutManager) :
        RecyclerView.OnScrollListener() {

    private var savedVisibleItemPosition = -1
    private var firstVisibleItemPosition: Int = 0
    private var visibleItemCount:Int = 0
    private var totalItemCount:Int = 0

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if(dy > 0) {
            // scrolled down
            visibleItemCount = mLayoutManager.childCount
            totalItemCount = mLayoutManager.itemCount
            firstVisibleItemPosition = mLayoutManager.findFirstVisibleItemPosition()

            if (savedVisibleItemPosition != firstVisibleItemPosition
                    && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount) {
                // load more data
                savedVisibleItemPosition = firstVisibleItemPosition
                onLoadMore()
            }
        }
    }

    protected abstract fun onLoadMore()
}