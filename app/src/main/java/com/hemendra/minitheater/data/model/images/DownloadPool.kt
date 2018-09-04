package com.hemendra.minitheater.data.model.images

import kotlin.collections.ArrayList

class DownloadPool {

    private val maxQueuedElements = 5

    private val downloadingSlots = Array<ImageLoader?>(10, init = { null })

    private val queue: ArrayList<ImageLoader> = ArrayList()

    fun newDownload(loader: ImageLoader) {
        if(!fitIntoFreeSlot(loader)) {
            if(!addToQueue(loader)) {
                fitIntoOldestSlot(loader)
            }
        }
    }

    private fun fitIntoFreeSlot(loader: ImageLoader): Boolean {
        for(i in 0 until downloadingSlots.size) {
            if(downloadingSlots[i] == null || downloadingSlots[i]?.isExecuting != true) {
                downloadingSlots[i] = loader
                loader.execute()
                return true
            }
        }
        return false
    }

    private fun addToQueue(loader: ImageLoader): Boolean {
        return if(queue.size < maxQueuedElements) {
            queue.add(loader)
            true
        } else false
    }

    private fun fitIntoOldestSlot(loader: ImageLoader) {
        var index1 = -1
        var timestamp1: Long = Long.MAX_VALUE
        for(i in 0 until downloadingSlots.size) {
            downloadingSlots[i]?.let {
                if(it.createdAt < timestamp1) {
                    timestamp1 = it.createdAt
                    index1 = i
                }
            }
        }
        var index2 = -1
        var timestamp2: Long = Long.MAX_VALUE
        for(i in 0 until queue.size) {
            if(queue[i].createdAt < timestamp2) {
                timestamp2 = queue[i].createdAt
                index2 = i
            }
        }
        if(index1 >= 0
                && (timestamp1 < timestamp2 || index2 < 0)) {
            downloadingSlots[index1]?.cancel(true)
            downloadingSlots[index1] = loader
            loader.execute()
        }
        else if(index2 >= 0) queue[index2] = loader
    }

    fun restackElementFromQueue() {
        var index = -1
        var timestamp: Long = Long.MAX_VALUE
        for (i in 0 until queue.size) {
            if (queue[i].createdAt < timestamp) {
                timestamp = queue[i].createdAt
                index = i
            }
        }
        if (index >= 0) {
            fitIntoFreeSlot(queue[index])
            queue.removeAt(index)
        }
    }

    fun abortAllDownloads() {
        queue.clear()
        for(i in 0 until downloadingSlots.size) {
            downloadingSlots[i]?.let {
                it.cancel(true)
            }
            downloadingSlots[i] = null
        }
    }

}