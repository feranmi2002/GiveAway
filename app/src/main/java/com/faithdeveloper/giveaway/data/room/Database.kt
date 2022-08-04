package com.faithdeveloper.giveaway.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.faithdeveloper.giveaway.data.models.UserProfile

@Database(entities = [UserProfile::class], version = 1)
abstract class Database: RoomDatabase() {
    abstract  fun usersDao(): UsersDao
//    abstract fun commentsDao():CommentsDao
}