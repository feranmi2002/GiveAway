package com.faithdeveloper.giveaway.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.liveData
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.data.models.PagerKey
import com.faithdeveloper.giveaway.pagingsources.ProfilePagingSource
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.LiveEvent
import kotlinx.coroutines.launch

class ProfileVM(val repository: Repository, private val getUserProfile: Boolean) : ViewModel() {
    private val _profilePicUpload = LiveEvent<Event>()
    val profilePicUpload get() = this._profilePicUpload
    private var _feedResult = loadFeed(
        uid = if (getUserProfile) getUserProfile().id
        else getAuthorProfile().id
    )
    val feedResult get() = _feedResult

    private fun loadFeed(uid: String) = Pager(
        config = PagingConfig(
            pageSize = 15
        ),
        pagingSourceFactory = {
            ProfilePagingSource(repository, true, uid)
        },
        initialKey = PagerKey(
            lastSnapshot = null,
            filter = repository.getTimelineOption()!!,
            loadSize = 10
        )
    ).liveData.cachedIn(viewModelScope)


    fun getTimelineOption() = repository.getTimelineOption()
    fun getUserProfile() = repository.getUserProfile()
    fun getAuthorProfile() = repository.getAuthorProfileForProfileView()
    fun uploadProfilePicture(profilePicPath: Uri) {
        viewModelScope.launch {
            profilePicPath.let {
                _profilePicUpload.postValue(repository.updateProfilePicture(it))
            }
        }
    }
}