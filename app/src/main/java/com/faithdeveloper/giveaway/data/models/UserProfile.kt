package com.faithdeveloper.giveaway.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.faithdeveloper.giveaway.utils.Extensions

@Entity(tableName = Extensions.USERS_DATABASE, indices = [Index(value = ["id"], unique = true)])
data class UserProfile(
     @PrimaryKey var id:String,
     @ColumnInfo var name:String,
     @ColumnInfo var phoneNumber:String,
     @ColumnInfo var email:String,
     @ColumnInfo var profilePicUrl:String,
     @ColumnInfo var reports:Int,
){
     constructor(): this("", "", "", "", "", -1)
}