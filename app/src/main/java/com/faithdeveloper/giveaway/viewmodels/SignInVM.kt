package com.faithdeveloper.giveaway.viewmodels

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faithdeveloper.giveaway.Event
import com.faithdeveloper.giveaway.LiveEvent
import com.faithdeveloper.giveaway.data.Repository
import kotlinx.coroutines.launch

class SignInVM(private val repository: Repository) : ViewModel() {
    private val _result = LiveEvent<Event>()
    val result get() = _result
    private val _timer = LiveEvent<String>()
    val timer get() = _timer

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _result.postValue(repository.forgotPassword(email))
        }
    }

    fun verifyEmail() {
        viewModelScope.launch {
            _result.postValue(repository.verifyEmail())
        }
    }

    fun signIn(email: String, password: String, userDetails: Array<String>) {
        viewModelScope.launch {
            if (repository.signIn(email, password, userDetails) is Event.Success) {
                if (repository.emailIsVerified() == true) _result.postValue(
                    Event.Success(
                        null,
                        "Sign in successful"
                    )
                )
                else _result.postValue(Event.Success(null, "Email unverified"))
            } else {
                _result.postValue(Event.Failure(null, "Sign in failed"))
            }
        }
    }


    fun startCounter() {
        val timer = object : CountDownTimer(120000, 1000) {
            override fun onTick(time: Long) {
                _timer.postValue((time.div(1000)).toString())
            }

            override fun onFinish() {
                // do nothing
            }
        }
        timer.start()
    }
}