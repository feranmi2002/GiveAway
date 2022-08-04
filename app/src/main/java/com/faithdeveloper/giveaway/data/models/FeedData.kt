package com.faithdeveloper.giveaway.data.models

data class FeedData(
    val postData: Post?,
    val authorData: UserProfile?
) {
    constructor() : this(null, null)
}
