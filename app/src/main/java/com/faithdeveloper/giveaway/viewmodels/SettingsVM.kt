package com.faithdeveloper.giveaway.viewmodels

import androidx.lifecycle.ViewModel
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.LiveEvent
import com.faithdeveloper.giveaway.data.Repository

class SettingsVM(val repository: Repository): ViewModel() {

    private val _result  = LiveEvent<Event>()
    val result get() = _result

    fun signUserOut(){
        _result.postValue(repository.signOut())
    }
}