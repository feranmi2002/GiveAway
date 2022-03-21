@file:Suppress("UNCHECKED_CAST")

package com.faithdeveloper.giveaway

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.viewmodels.FeedVM
import com.faithdeveloper.giveaway.viewmodels.SignInVM
import com.faithdeveloper.giveaway.viewmodels.SignUpVM

class VMFactory(private val repository: Repository) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignUpVM::class.java))
            return SignUpVM(repository) as T
        if (modelClass.isAssignableFrom(SignInVM::class.java))
            return SignInVM(repository) as T
        if (modelClass.isAssignableFrom(FeedVM::class.java))
            return FeedVM(repository) as T
        else throw IllegalArgumentException("Unknown Class")
    }
}