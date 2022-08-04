package com.faithdeveloper.giveaway.data.models

import com.google.firebase.firestore.DocumentSnapshot

data class FirebaseModelComments(
    val postData: DocumentSnapshot?,
    val posterData: DocumentSnapshot?,
    val posterRepliedTo:DocumentSnapshot?
) {
    constructor() : this(null, null, null)
}
