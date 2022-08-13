package com.faithdeveloper.giveaway.data.models

import androidx.annotation.VisibleForTesting
import com.google.firebase.firestore.DocumentSnapshot

data class PagerKey(
    val lastSnapshot: DocumentSnapshot?,
    val filter:String = "All",
    val loadSize:Long
){
    constructor():this(null, "All", 0)
}