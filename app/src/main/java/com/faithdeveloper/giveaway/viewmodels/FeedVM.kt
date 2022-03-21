package com.faithdeveloper.giveaway.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faithdeveloper.giveaway.Event
import com.faithdeveloper.giveaway.LiveEvent
import com.faithdeveloper.giveaway.data.Repository
import kotlinx.coroutines.launch

class FeedVM(private val repository: Repository): ViewModel() {
    private val _homeResult = LiveEvent<Event>()
    val homeResult get() = _homeResult
    fun checkIfUserDetailIsStored() {
//        viewModelScope.launch {
//            if (!repository.userDetailsIsStored()) {
//                repository.storeUserDetails()
//            }
//        }
    }

    fun uploadProfilePicture(uriContent: Uri?) {
        viewModelScope.launch {
            uriContent?.let {
                _homeResult.postValue(repository.createProfilePicture(it))
            }
        }

    }

}
