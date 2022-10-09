package com.faithdeveloper.giveaway.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.LiveEvent
import kotlinx.coroutines.launch

class ProfileEditVM(val repository: Repository) : ViewModel() {
    private val _result = LiveEvent<Event>()
    val result get() = _result
    private var _newPicture: Uri? = null
    val newPicture get() = _newPicture

    fun getUserDetails() = repository.userDetails()
    fun getUserProfilePic() = repository.getUserProfilePicUrl()
    fun newPicture(pictureUri: Uri) {
        _newPicture = pictureUri
    }

    fun updateProfile(name: String, phone: String?) {
        viewModelScope.launch {
            _result.postValue(repository.updateProfile(name, phone, newPicture))
        }
    }
}
