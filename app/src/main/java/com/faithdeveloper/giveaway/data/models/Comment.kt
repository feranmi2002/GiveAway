package com.faithdeveloper.giveaway.data.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.*

//@Entity(tableName = "CommentsTable", indices = [Index(value = ["localDbId"], unique = true)])
data class Comment(
    var authorId:String,
    var commentText:String,
    var id:String,
    @ServerTimestamp
    val time: Date?,
    var parentID:String,
    var replies:Int,
    var updated:Boolean
){
    constructor(): this("", "", "", Date(), "", 0, false)
}
/*
@Entity(tableName = "CommentsTable", indices = [Index(value = ["localDbId"], unique = true)])
data class Comments(
    @PrimaryKey(autoGenerate = true) var localDbId:Int,
    @ColumnInfo var commentPosterUid:String,
    @ColumnInfo var commentText:String,
    @ColumnInfo var time:Long,
    @ColumnInfo var postID:String,
    @ColumnInfo var commentOwner:Boolean,
    @ColumnInfo var replyTo:String,
    @ColumnInfo var replyToUid:String,
    @ColumnInfo  var posterId:String
){
    constructor(): this( 0,"", "", 0, "", false, "", "", "")
}

*/
