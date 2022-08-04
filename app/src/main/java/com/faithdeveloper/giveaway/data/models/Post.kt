package com.faithdeveloper.giveaway.data.models

data class Post(
    val authorId: String = "",
    val postID: String,
    val time: Long,
    val text: String,
    val tags: List<String>,
    val mediaUrls: List<String>,
    val hasComments: Boolean,
    val hasVideo: Boolean,
    val link: String
) {
    constructor() : this("", "", -1, "", listOf<String>(), listOf<String>(), false, false, "")
}
