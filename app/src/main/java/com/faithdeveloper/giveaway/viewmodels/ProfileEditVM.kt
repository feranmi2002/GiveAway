package com.faithdeveloper.giveaway.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.LiveEvent
import com.faithdeveloper.giveaway.data.Repository
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
        _result.postValue(Event.Success("New picture"))
    }

    fun updateProfile(profileEditType: String, phone: String?, name: String?) {
        viewModelScope.launch {
            _result.postValue(repository.updateProfile(profileEditType, phone, name, newPicture))
        }

    }


}
