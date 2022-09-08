package com.faithdeveloper.giveaway.viewmodels

import androidx.lifecycle.*
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.liveData
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.data.models.PagerKey
import com.faithdeveloper.giveaway.data.models.UserProfile
import com.faithdeveloper.giveaway.pagingsources.RepliesPagingSource
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.LiveEvent
import com.faithdeveloper.giveaway.viewmodels.FeedVM.Companion.DEFAULT_FILTER
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RepliesVM(private val repository: Repository) : ViewModel() {
    private var parentId = MutableLiveData<String>()
    private var commentID: String = ""
    private val _actionResult = LiveEvent<Event>()
    val actionResult get() = _actionResult
    private val _feedResult = parentId.distinctUntilChanged().switchMap {
        loadFeed(it)
    }
    val feedResult get() = _feedResult
    fun setNeededData(parentId: String, commentId: String) {
        commentID = commentId
        this.parentId.value = parentId
    }


    private fun loadFeed(parentId: String) = Pager(
        config = PagingConfig(
            pageSize = 15
        ), pagingSourceFactory = {
            RepliesPagingSource(repository, parentId, commentID)
        }, initialKey = PagerKey(null, DEFAULT_FILTER, 10)
    ).liveData.cachedIn(viewModelScope)


    fun userUid() = repository.userUid()!!

    fun uploadReply(userProfile: UserProfile?, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _actionResult.postValue(repository.addAReply(text, parentId.value!!, commentID, userProfile))
        }
    }

    fun editReply(id: String, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _actionResult.postValue(repository.updateReply(text, parentId.value!!, commentID, id))
        }
    }

    fun deleteReply(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _actionResult.postValue(repository.deleteReply(parentId.value!!, commentID, id))
        }
    }

    fun addNewReply(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _actionResult.postValue(repository.addAReply(text, parentId.value!!, commentID, null))
        }
    }
}