package com.faithdeveloper.giveaway.data.models

data class CommentProfiles(
    var author: UserProfile?,
    var userRepliedTo: UserProfile?
){
    constructor(): this (null, null)
}