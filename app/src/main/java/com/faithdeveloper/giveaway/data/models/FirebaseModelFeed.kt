package com.faithdeveloper.giveaway.data.models

import com.google.firebase.firestore.DocumentSnapshot

data class FirebaseModelFeed(
    val postData: DocumentSnapshot?,
    val authorData: DocumentSnapshot?
) {
    constructor() : this(null, null)
}
