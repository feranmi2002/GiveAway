package com.faithdeveloper.giveaway.data.models

data class ReplyData(
    val reply: Reply?,
    val author: UserProfile?,
    val userReplied:UserProfile?
) {
    constructor() : this(null, null, null)
}
