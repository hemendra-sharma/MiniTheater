package com.hemendra.minitheater.data.model.movies

enum class MoviesDataSourceFailureReason {

    ALREADY_LOADING,
    NO_INTERNET_CONNECTION,
    NETWORK_TIMEOUT,
    ABORTED,
    NO_SEARCH_RESULTS,
    API_MISSING,
    UNKNOWN

}