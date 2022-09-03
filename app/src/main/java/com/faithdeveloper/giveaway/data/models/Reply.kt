package com.faithdeveloper.giveaway.data.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class Reply(
    var authorId:String,
    var commentText:String,
    var id:String,
    @ServerTimestamp
    val time: Date?,
    var idOfTheUserThisCommentIsAReplyTo:String,
    var updated:Boolean
){
    constructor(): this("", "", "", Date(), "", false)
}

