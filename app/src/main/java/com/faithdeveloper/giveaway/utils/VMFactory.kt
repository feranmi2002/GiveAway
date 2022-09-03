@file:Suppress("UNCHECKED_CAST")

package com.faithdeveloper.giveaway.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.viewmodels.*
class VMFactory(private val repository: Repository, val postID:String?=null, val getUserProfile:Boolean = true) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignUpVM::class.java))
            return SignUpVM(repository) as T
        if (modelClass.isAssignableFrom(SignInVM::class.java))
            return SignInVM(repository) as T
        if (modelClass.isAssignableFrom(FeedVM::class.java))
            return FeedVM(repository) as T
        if (modelClass.isAssignableFrom(NewPostVM::class.java))
            return NewPostVM(repository) as T
        if (modelClass.isAssignableFrom(CommentsVM::class.java))
            return CommentsVM(repository, postID!!) as T
        if (modelClass.isAssignableFrom(RepliesVM::class.java))
            return RepliesVM(repository) as T
        if (modelClass.isAssignableFrom(ProfileVM::class.java))
            return ProfileVM(repository, getUserProfile) as T
        if (modelClass.isAssignableFrom(SettingsVM::class.java))
            return SettingsVM(repository) as T
        if (modelClass.isAssignableFrom(ProfileEditVM::class.java))
            return ProfileEditVM(repository) as T
        if (modelClass.isAssignableFrom(SearchVM::class.java))
            return SearchVM(repository) as T
        if (modelClass.isAssignableFrom(UserUnverifiedVM::class.java))
            return UserUnverifiedVM(repository) as T
        else throw IllegalArgumentException("Unknown Class")
    }
}