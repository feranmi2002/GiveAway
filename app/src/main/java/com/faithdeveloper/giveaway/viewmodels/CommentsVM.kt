package com.faithdeveloper.giveaway.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.liveData
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.data.models.CommentData
import com.faithdeveloper.giveaway.data.models.PagerKey
import com.faithdeveloper.giveaway.pagingsources.CommentsPagingSource
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.LiveEvent
import com.faithdeveloper.giveaway.viewmodels.FeedVM.Companion.DEFAULT_FILTER
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CommentsVM(private val repository: Repository, private val parentID: String) : ViewModel() {
    private val _feedResult = loadFeed()
    private val _commentActionResult = LiveEvent<Event>()
    val commentActionResult get() = _commentActionResult
    val feedResult get() = _feedResult

    private fun loadFeed() = Pager(
        config = PagingConfig(
            pageSize = 15
        ), pagingSourceFactory = {
            CommentsPagingSource(repository, parentID)
        }, initialKey = PagerKey(null, DEFAULT_FILTER, 10)
    ).liveData.cachedIn(viewModelScope)


    fun userUid() = repository.userUid()!!


    fun deleteComment(commentID: String, replies:Int) {
        viewModelScope.launch {
            _commentActionResult.postValue(repository.deleteComment(parentID, commentID,replies))
        }
    }

    fun editComment(commentData: CommentData?) {
        viewModelScope.launch(Dispatchers.IO) {
            _commentActionResult.postValue(repository.updateComment(parentID, commentData))
        }
    }

    fun addNewComment(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _commentActionResult.postValue(repository.addNewComment(text, parentID))
        }
    }
}