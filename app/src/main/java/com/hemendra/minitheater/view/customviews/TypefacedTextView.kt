package com.hemendra.minitheater.view.customviews

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.TextView
import com.hemendra.minitheater.R

class TypefacedTextView(context: Context,
                        attributeSet: AttributeSet? = null,
                        style: Int = 0):
        TextView(context,  attributeSet, style) {

    init {
        if(attributeSet != null) {
            val a = context.obtainStyledAttributes(attributeSet, R.styleable.CustomTextView)
            val fontName = a.getString(R.styleable.CustomTextView_fontTTF)
            a.recycle()
            fontName?.length?.let {
                if(it > 0) {
                    val tf = Typeface.createFromAsset(resources.assets, fontName)
                    if(tf != null) typeface = tf
                }
            }
        }
    }
}