package com.faithdeveloper.giveaway.data.models

import androidx.annotation.VisibleForTesting
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp

data class FeedPagerKey(
    val lastSnapshot: DocumentSnapshot?,
    val filter:String = "All",
    val loadSize:Long,
    val latestTimestamp: Timestamp?
){
    constructor():this(null, "All", 0, null)
}