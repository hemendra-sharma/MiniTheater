package com.hemendra.minitheater.data

import java.io.Serializable

class Torrent: Serializable {

    companion object {
        const val serialVersionUID: Long = 7823482933L
    }

    var url: String = ""
    var hash: String = ""
    var quality: String = ""
    var seeds: Int = 0
    var peers: Int = 0
    var size: String = ""
    var size_bytes: Long = 0 // example: 922285507
    var date_uploaded: String = "" // example: 2018-08-29 18:51:27
    var date_uploaded_unix: Long = 0 // example: 1535561487

}