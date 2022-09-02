package com.faithdeveloper.giveaway.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.liveData
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.data.models.PagerKey
import com.faithdeveloper.giveaway.pagingsources.ProfilePagingSource

class ProfileVM(val repository: Repository, private val getUserProfile: Boolean) : ViewModel() {
    var adapterIsSetUp: Boolean = false
    private var _feedResult = loadFeed(
        uid = if (getUserProfile) getUserProfile().id
        else getAuthorProfile().id
    )
    val feedResult get() = _feedResult


    fun setTimeLineOption(timeLineOption: String) {
        repository.setTimeLineOption(timeLineOption)
    }

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
}