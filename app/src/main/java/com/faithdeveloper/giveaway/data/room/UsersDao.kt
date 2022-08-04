package com.faithdeveloper.giveaway.data.room

import androidx.room.*
import com.faithdeveloper.giveaway.utils.Extensions
import com.faithdeveloper.giveaway.data.models.UserProfile

@Dao
interface UsersDao {

    @Query("SELECT * FROM $database")
    fun getAll(): List<UserProfile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(vararg users: UserProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllUsers(users: List<UserProfile>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateUsers(vararg users: UserProfile)

    @Delete
    fun delete(vararg users: UserProfile)

    @Query("SELECT * FROM $database WHERE $name LIKE :nameQuery")
    fun searchUsers(nameQuery: String): List<UserProfile>

    @Query("SELECT * FROM $database WHERE $id LIKE :userID")
    fun getUser(userID:String): List<UserProfile>

    companion object {
        const val database = Extensions.USERS_DATABASE
        const val id = "id"
        const val name = "name"
    }
}