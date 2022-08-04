package com.faithdeveloper.giveaway.data.models

import com.google.firebase.firestore.DocumentSnapshot

data class PagerResponse<T>(
    val data:List<T>,
    val lastSnapshot:DocumentSnapshot?

)
