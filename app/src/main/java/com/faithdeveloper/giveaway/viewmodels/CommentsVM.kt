package com.faithdeveloper.giveaway.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.liveData
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.LiveEvent
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.data.models.FeedPagerKey
import com.faithdeveloper.giveaway.data.models.UserProfile
import com.faithdeveloper.giveaway.pagingsources.CommentsPagingSource
import com.faithdeveloper.giveaway.ui.fragments.CommentsBottomSheet.Companion.POST
import com.faithdeveloper.giveaway.viewmodels.FeedVM.Companion.DEFAULT_FILTER
import kotlinx.coroutines.launch

class CommentsVM(private val repository: Repository, val postID:String) : ViewModel() {
    private val _feedResult = loadFeed()
    private val _commentActionResult = LiveEvent<Event>()
    private var _adapterIsSetUp = false
    val adapterIsSetUp get() = _adapterIsSetUp
    val commentActionResult get() = _commentActionResult
    val feedResult get() = _feedResult

    fun setAdapterState(state: Boolean) {
        _adapterIsSetUp = state
    }

    private fun loadFeed() = Pager(
        config = PagingConfig(
            pageSize = 15,
            maxSize = 30,
            enablePlaceholders = false,
            prefetchDistance = 5
        ), pagingSourceFactory = {
            CommentsPagingSource(repository, true, postID)
        }, initialKey = FeedPagerKey(null, DEFAULT_FILTER, 10)
    ).liveData.cachedIn(viewModelScope)


    fun userUid() = repository.userUid()!!

    fun addOrUpdateComment(
        text: String,
        profileOfUserThisCommentIsAReplyTo: UserProfile?,
        action: String
    ) {
        viewModelScope.launch {
            if (action == POST) {
                _commentActionResult.postValue(
                    repository.addNewComment(
                        text,
                        postID,
                        profileOfUserThisCommentIsAReplyTo
                    )
                )
            } else {
                _commentActionResult.postValue(
                    repository.updateComment(
                        text,
                        postID
                    )
                )
            }
        }

    }

  fun deleteComment(postID: String) {
      viewModelScope.launch {
          _commentActionResult.postValue(repository.deleteComment(postID))
      }
  }
}