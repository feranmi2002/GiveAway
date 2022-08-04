package com.faithdeveloper.giveaway.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.LiveEvent
import com.faithdeveloper.giveaway.data.Repository
import kotlinx.coroutines.launch

class SignUpVM(private val repository: Repository): ViewModel() {
    private val _result = LiveEvent<Event>()
    val result get() = _result
    fun signUp(phone: String, name: String, email: String, password:String) {
        viewModelScope.launch {
            _result.postValue(repository.signUp(phone, name, email, password))
        }

    }

    fun verifyEmailAddress() {
        viewModelScope.launch {
            _result.postValue(repository.verifyEmail())
        }
    }


}