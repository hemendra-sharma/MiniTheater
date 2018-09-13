package com.hemendra.minitheater.view.player

import android.content.Context
import android.database.DataSetObserver
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.TextView
import com.hemendra.minitheater.data.Subtitle
import java.util.*

class SubtitlesListAdapter(context: Context, var list: ArrayList<Subtitle>):
        ArrayAdapter<Subtitle>(context, 0) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val params = AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,
                AbsListView.LayoutParams.WRAP_CONTENT)

        val tv = TextView(context)
        tv.layoutParams = params
        tv.setTextColor(Color.WHITE)
        tv.setPadding(10, 15, 10, 15)
        tv.gravity = Gravity.CENTER_VERTICAL or Gravity.START
        tv.text = String.format(Locale.getDefault(), "${position+1}.\t %s (%d)",
                list[position].language, list[position].count)
        tv.textSize = 16f

        return tv
    }

    override fun isEmpty(): Boolean = list.isEmpty()

    override fun getItemViewType(position: Int): Int = 0

    override fun getItem(position: Int): Subtitle = list[position]

    override fun getViewTypeCount(): Int = 1

    override fun isEnabled(position: Int): Boolean = true

    override fun getItemId(position: Int): Long = position.toLong()

    override fun areAllItemsEnabled(): Boolean = true

    override fun getCount(): Int = list.size
}