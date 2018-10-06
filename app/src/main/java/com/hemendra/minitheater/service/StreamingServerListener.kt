package com.hemendra.minitheater.service

interface StreamingServerListener {
    fun started(address: String)
    fun failedToStart()
}