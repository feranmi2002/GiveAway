package com.faithdeveloper.giveaway.data.models

import com.google.firebase.Timestamp
import com.google.firebase.database.ServerValue
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class Post(
    val authorId: String = "",
    val postID: String,
    @ServerTimestamp
    val time: Date? ,
    val text: String,
    val tags: List<String>,
    val mediaUrls: List<String>,
    val hasComments: Boolean,
    val hasVideo: Boolean,
    val link: String
) {
    constructor() : this("", "", Date(), "", listOf<String>(), listOf<String>(), false, false, "")
}
