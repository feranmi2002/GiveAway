package com.faithdeveloper.giveaway.utils.interfaces

import com.google.firebase.Timestamp

interface FeedVMAndPagingSourceInterface {

    fun latestFeedTimestamp(timeStamp: Long)

    fun clearViewModelPreloadedData()
}