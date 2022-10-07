package com.faithdeveloper.giveaway.utils.interfaces

import com.faithdeveloper.giveaway.data.models.FeedData
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

interface FeedVMAndPagingSourceInterface {


    fun updateLatestFeedTimeStamp(timeStamp: Long)

    fun clearViewModelPreloadedData()

    fun requestCachedData(filter:String):List<FeedData>?

    fun storeLastSnapshot(filter:String, snapshot:DocumentSnapshot?)

    fun getLastSnapshot(filter:String):DocumentSnapshot?

//    fun getExplicitRefresh():Boolean

//    fun mSetExplicitRefresh(value:Boolean)

    fun getCurrentFilter():String

    fun clearCachedData(filter:String)

}