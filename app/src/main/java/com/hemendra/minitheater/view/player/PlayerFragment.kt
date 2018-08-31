package com.hemendra.minitheater.view.player

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hemendra.minitheater.R

class PlayerFragment: Fragment() {

    companion object {
        private var playerFragment: PlayerFragment? = null

        fun getInstance(): PlayerFragment {
            playerFragment?.let {
                return@let playerFragment
            }
            playerFragment = PlayerFragment()
            return playerFragment as PlayerFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}