package com.faithdeveloper.giveaway.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class FeedPagerResponse<T>(
    val data:List<T>,
    val lastSnapshot:DocumentSnapshot?,
    val latestSnapshotTimestamp:Timestamp

)
