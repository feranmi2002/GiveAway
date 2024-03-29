package com.faithdeveloper.giveaway.viewmodels

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.LiveEvent
import com.faithdeveloper.giveaway.data.Repository
import kotlinx.coroutines.launch

class SignInVM(private val repository: Repository) : ViewModel() {
    private val _result = LiveEvent<Event>()
    val result get() = _result
    private val _timer = LiveEvent<Long>()
    val timer get() = _timer

    fun forgotPassword(email: String) {
        viewModelScope.launch {
             _result.postValue(repository.forgotPassword(email))
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _result.postValue(repository.signIn(email, password))
        }
    }

    fun startCounter() {
        val timer = object : CountDownTimer(120000, 1000) {
            override fun onTick(time: Long) {
                _timer.postValue(time)
            }

            override fun onFinish() {
                // do nothing
            }
        }
        timer.start()
    }
}