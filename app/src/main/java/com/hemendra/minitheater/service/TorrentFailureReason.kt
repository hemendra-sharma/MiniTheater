package com.hemendra.minitheater.service

import java.io.Serializable

enum class TorrentFailureReason: Serializable {
    NO_INTERNET_CONNECTION,
    ABORTED,
    NOTHING_TO_DOWNLOAD,
    UNKNOWN
}