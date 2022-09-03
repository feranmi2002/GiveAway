package com.faithdeveloper.giveaway.data.models

data class ReplyProfiles(
    var author: UserProfile?,
    var userRepliedTo: UserProfile?
){
    constructor(): this (null, null)
}