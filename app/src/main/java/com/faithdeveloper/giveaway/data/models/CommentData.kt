package com.faithdeveloper.giveaway.data.models

data class CommentData(
    val comment: Comment?,
    val author: UserProfile?,
    val userRepliedTo: UserProfile?
) {
    constructor() : this(null, null, null)
}
